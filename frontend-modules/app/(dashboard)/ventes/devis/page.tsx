"use client";

import { useEffect, useState, useMemo } from "react";
import {
  FileText,
  Plus,
  ArrowRightCircle,
  FileCheck,
  CheckCircle2,
  XCircle,
  Calendar,
  DollarSign,
  User,
  Trash2,
  Printer,
  RefreshCw,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { DataTable, Column } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { devisApi } from "@/lib/api/devis";
import { clientsApi } from "@/lib/api/clients";
import { produitsApi } from "@/lib/api/produits";
import { useAuth } from "@/providers/auth-provider";
import { toast } from "sonner";
import type { Devis, LigneDevis, StatutDevis } from "@/types/commercial";
import type { Client, Produit } from "@/types/api";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function DevisPage() {
  const [devisList, setDevisList] = useState<Devis[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [produits, setProduits] = useState<Produit[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { user } = useAuth();

  // Form State
  const [selectedClientId, setSelectedClientId] = useState<string>("");
  const [dateValidite, setDateValidite] = useState<string>(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]
  );
  const [notes, setNotes] = useState<string>("");
  const [lignes, setLignes] = useState<Array<LigneDevis>>([]);

  function loadData() {
    setLoading(true);
    Promise.all([devisApi.getAllDevis(), clientsApi.getAllClients(), produitsApi.getAllProduits()])
      .then(([dev, cli, prod]) => {
        setDevisList(dev || []);
        setClients(cli || []);
        setProduits(prod || []);
      })
      .catch((err) => {
        console.error(err);
        toast.error("Erreur lors du chargement des devis");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  // Totaux du formulaire
  const { totalHT, totalTVA, totalTTC } = useMemo(() => {
    let ht = 0;
    let tva = 0;
    lignes.forEach((l) => {
      const brut = (l.prixUnitaireHT || 0) * (l.quantite || 0);
      const remise = brut * ((l.tauxRemise || 0) / 100);
      const ligneHT = brut - remise;
      const ligneTVA = ligneHT * ((l.tauxTVA || 20) / 100);
      ht += ligneHT;
      tva += ligneTVA;
    });
    return { totalHT: ht, totalTVA: tva, totalTTC: ht + tva };
  }, [lignes]);

  function handleAddLine() {
    if (!produits.length) return;
    const firstProd = produits[0];
    const prix = firstProd.prixVenteHt || firstProd.prixVente || 0;
    setLignes((prev) => [
      ...prev,
      {
        produit: firstProd,
        produitId: firstProd.id,
        quantite: 1,
        prixUnitaireHT: prix,
        tauxTVA: 20,
        tauxRemise: 0,
        montantHT: prix,
        montantTTC: prix * 1.2,
      },
    ]);
  }

  function handleLineProductChange(index: number, prodId: number) {
    const prod = produits.find((p) => p.id === prodId);
    if (!prod) return;
    const prix = prod.prixVenteHt || prod.prixVente || 0;
    setLignes((prev) => {
      const copy = [...prev];
      copy[index] = {
        ...copy[index],
        produit: prod,
        produitId: prod.id,
        prixUnitaireHT: prix,
        montantHT: prix * (copy[index].quantite || 1),
        montantTTC: prix * (copy[index].quantite || 1) * 1.2,
      };
      return copy;
    });
  }

  function handleLineChange(index: number, field: keyof LigneDevis, val: number) {
    setLignes((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: val };
      return copy;
    });
  }

  function handleRemoveLine(index: number) {
    setLignes((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleCreateDevis() {
    if (!selectedClientId) {
      toast.error("Veuillez sélectionner un client");
      return;
    }
    if (!lignes.length) {
      toast.error("Veuillez ajouter au moins un article");
      return;
    }

    try {
      const newDevis: Partial<Devis> = {
        client: { id: parseInt(selectedClientId) } as Client,
        dateValidite,
        notes,
        lignes: lignes.map((l) => ({
          produit: { id: l.produit?.id || l.produitId } as Produit,
          quantite: l.quantite,
          prixUnitaireHT: l.prixUnitaireHT,
          tauxTVA: l.tauxTVA,
          tauxRemise: l.tauxRemise,
        })),
      };

      await devisApi.creerDevis(newDevis, user?.id);
      toast.success("Devis créé avec succès !");
      setIsModalOpen(false);
      setLignes([]);
      setNotes("");
      loadData();
    } catch (err: any) {
      console.error(err);
      toast.error("Erreur lors de la création du devis : " + (err.message || ""));
    }
  }

  async function handleConvertToCommande(id: number) {
    try {
      await devisApi.convertirEnCommande(id, user?.id);
      toast.success("Devis transformé en Commande Client !");
      loadData();
    } catch (err: any) {
      toast.error("Erreur lors de la conversion en commande : " + err.message);
    }
  }

  async function handleConvertToFacture(id: number) {
    try {
      await devisApi.convertirEnFacture(id, user?.id);
      toast.success("Devis converti directement en Facture !");
      loadData();
    } catch (err: any) {
      toast.error("Erreur lors de la conversion en facture : " + err.message);
    }
  }

  async function handleChangeStatut(id: number, statut: StatutDevis) {
    try {
      await devisApi.changerStatut(id, statut);
      toast.success(`Statut mis à jour : ${statut}`);
      loadData();
    } catch (err: any) {
      toast.error("Erreur changement de statut");
    }
  }

  // KPIs
  const totalDevisCount = devisList.length;
  const montantTotalGlobal = devisList.reduce((acc, d) => acc + (d.montantFinal || d.montantTTC || 0), 0);
  const devisEnAttente = devisList.filter((d) => d.statut === "BROUILLON" || d.statut === "ENVOYE").length;
  const devisConvertis = devisList.filter((d) => d.statut === "TRANSFORME_EN_COMMANDE" || d.statut === "ACCEPTE").length;

  const columns: Column<Devis>[] = [
    {
      header: "N° Devis",
      accessor: "numeroDevis",
      cell: (row) => (
        <div className="flex items-center gap-2">
          <FileText className="h-4 w-4 text-primary" />
          <span className="font-semibold text-foreground">{row.numeroDevis}</span>
        </div>
      ),
    },
    {
      header: "Date",
      accessor: "dateDevis",
      cell: (row) => formatDate(row.dateDevis),
    },
    {
      header: "Validité",
      accessor: "dateValidite",
      cell: (row) => (
        <span className="text-muted-foreground text-xs">{formatDate(row.dateValidite)}</span>
      ),
    },
    {
      header: "Client",
      accessor: "client",
      cell: (row) => (
        <div>
          <div className="font-medium text-foreground">{row.client?.nomComplet || row.client?.nom || "—"}</div>
          <div className="text-muted-foreground text-xs">{row.client?.telephone || row.client?.email || ""}</div>
        </div>
      ),
    },
    {
      header: "Montant HT",
      accessor: "montantHT",
      cell: (row) => formatMAD(row.montantHT),
    },
    {
      header: "Montant TTC",
      accessor: "montantFinal",
      cell: (row) => <span className="font-bold text-foreground">{formatMAD(row.montantFinal || row.montantTTC)}</span>,
    },
    {
      header: "Statut",
      accessor: "statut",
      cell: (row) => <StatusBadge status={row.statut as any} />,
    },
    {
      header: "Actions",
      accessor: "id",
      className: "text-right",
      cell: (row) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm">
              Actions
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>Actions Devis</DropdownMenuLabel>
            <DropdownMenuItem onClick={() => row.id && handleConvertToCommande(row.id)}>
              <ArrowRightCircle className="mr-2 h-4 w-4 text-blue-500" />
              Convertir en Commande
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => row.id && handleConvertToFacture(row.id)}>
              <FileCheck className="mr-2 h-4 w-4 text-emerald-500" />
              Convertir en Facture
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => row.id && handleChangeStatut(row.id, "ACCEPTE" as StatutDevis)}>
              <CheckCircle2 className="mr-2 h-4 w-4 text-green-600" />
              Marquer Accepté
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => row.id && handleChangeStatut(row.id, "REFUSE" as StatutDevis)}>
              <XCircle className="mr-2 h-4 w-4 text-red-500" />
              Marquer Refusé
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => window.print()}>
              <Printer className="mr-2 h-4 w-4" />
              Imprimer / PDF
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Devis Clients"
        description="Créez, suivez et convertissez vos devis commerciaux en commandes ou factures en 1 clic."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button size="sm" onClick={() => { setIsModalOpen(true); handleAddLine(); }}>
            <Plus className="mr-2 h-4 w-4" />
            Nouveau Devis
          </Button>
        </div>
      </PageHeader>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Devis"
          value={totalDevisCount}
          description="Tous statuts confondus"
          icon={FileText}
          color="blue"
        />
        <StatCard
          title="Volume Devis (TTC)"
          value={formatMAD(montantTotalGlobal)}
          description="Montant cumulé"
          icon={DollarSign}
          color="green"
        />
        <StatCard
          title="En Négociation / Attente"
          value={devisEnAttente}
          description="Brouillons ou envoyés"
          icon={Calendar}
          color="orange"
        />
        <StatCard
          title="Gagnés / Convertis"
          value={devisConvertis}
          description="Commandes & Factures créées"
          icon={CheckCircle2}
          color="purple"
        />
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={devisList}
        loading={loading}
        searchKey="numeroDevis"
        searchPlaceholder="Rechercher par numéro de devis..."
      />

      {/* Modal Création Devis */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-xl font-bold">
              <FileText className="h-5 w-5 text-primary" />
              Créer un Nouveau Devis
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-1">
                <Label htmlFor="client">Client *</Label>
                <select
                  id="client"
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  value={selectedClientId}
                  onChange={(e) => setSelectedClientId(e.target.value)}
                >
                  <option value="">-- Sélectionner un client --</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nomComplet || c.nom} ({c.telephone || c.email || "Sans tél"})
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-1">
                <Label htmlFor="validite">Date de Validité</Label>
                <Input
                  id="validite"
                  type="date"
                  value={dateValidite}
                  onChange={(e) => setDateValidite(e.target.value)}
                />
              </div>
            </div>

            {/* Articles Table */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label className="text-base font-semibold">Articles & Prestations</Label>
                <Button type="button" variant="outline" size="sm" onClick={handleAddLine}>
                  <Plus className="mr-1 h-3 w-3" />
                  Ajouter une ligne
                </Button>
              </div>

              <div className="rounded-md border overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-muted/50 text-xs font-semibold text-muted-foreground uppercase border-b">
                    <tr>
                      <th className="px-3 py-2 text-left">Article</th>
                      <th className="px-3 py-2 text-right w-24">Quantité</th>
                      <th className="px-3 py-2 text-right w-28">Prix HT (MAD)</th>
                      <th className="px-3 py-2 text-right w-20">Remise %</th>
                      <th className="px-3 py-2 text-right w-28">Total TTC</th>
                      <th className="px-2 py-2 text-center w-12"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {lignes.map((line, idx) => (
                      <tr key={idx}>
                        <td className="p-2">
                          <select
                            className="w-full rounded border bg-background p-1 text-xs"
                            value={line.produit?.id || line.produitId}
                            onChange={(e) => handleLineProductChange(idx, parseInt(e.target.value))}
                          >
                            {produits.map((p) => (
                              <option key={p.id} value={p.id}>
                                {p.reference} - {p.designation}
                              </option>
                            ))}
                          </select>
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            min="1"
                            className="h-8 text-right text-xs"
                            value={line.quantite}
                            onChange={(e) => handleLineChange(idx, "quantite", parseFloat(e.target.value) || 0)}
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            step="0.01"
                            className="h-8 text-right text-xs"
                            value={line.prixUnitaireHT}
                            onChange={(e) => handleLineChange(idx, "prixUnitaireHT", parseFloat(e.target.value) || 0)}
                          />
                        </td>
                        <td className="p-2">
                          <Input
                            type="number"
                            min="0"
                            max="100"
                            className="h-8 text-right text-xs"
                            value={line.tauxRemise || 0}
                            onChange={(e) => handleLineChange(idx, "tauxRemise", parseFloat(e.target.value) || 0)}
                          />
                        </td>
                        <td className="p-2 text-right font-medium text-xs">
                          {formatMAD(
                            ((line.prixUnitaireHT || 0) * (line.quantite || 0) * (1 - (line.tauxRemise || 0) / 100)) *
                              1.2
                          )}
                        </td>
                        <td className="p-2 text-center">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-red-500 hover:text-red-700"
                            onClick={() => handleRemoveLine(idx)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </td>
                      </tr>
                    ))}
                    {!lignes.length && (
                      <tr>
                        <td colSpan={6} className="p-4 text-center text-muted-foreground text-xs">
                          Aucun article ajouté. Cliquez sur "Ajouter une ligne".
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Totaux */}
            <div className="flex justify-end pt-2">
              <div className="w-64 space-y-1 text-sm bg-muted/30 p-3 rounded-lg border">
                <div className="flex justify-between text-muted-foreground">
                  <span>Total HT :</span>
                  <span>{formatMAD(totalHT)}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>TVA (20%) :</span>
                  <span>{formatMAD(totalTVA)}</span>
                </div>
                <div className="flex justify-between font-bold text-foreground text-base pt-1 border-t">
                  <span>Total TTC :</span>
                  <span className="text-primary">{formatMAD(totalTTC)}</span>
                </div>
              </div>
            </div>

            <div className="space-y-1">
              <Label htmlFor="notes">Conditions & Remarques</Label>
              <Input
                id="notes"
                placeholder="Ex: Conditions de règlement 30 jours, validité 1 mois..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleCreateDevis}>
              Enregistrer le devis
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
