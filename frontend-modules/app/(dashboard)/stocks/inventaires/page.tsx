"use client";

import { useEffect, useState } from "react";
import {
  Boxes,
  Plus,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  Calendar,
  Warehouse,
  Save,
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
import { inventairesApi } from "@/lib/api/inventaires";
import { depotsApi } from "@/lib/api/depots";
import { useAuth } from "@/providers/auth-provider";
import { toast } from "sonner";
import { StatutInventaire, type Inventaire, type LigneInventaire } from "@/types/commercial";
import type { Depot } from "@/types/api";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function InventairesPage() {
  const [inventaires, setInventaires] = useState<Inventaire[]>([]);
  const [depots, setDepots] = useState<Depot[]>([]);
  const [loading, setLoading] = useState(true);
  const [isStartModalOpen, setIsStartModalOpen] = useState(false);
  const [isCountModalOpen, setIsCountModalOpen] = useState(false);
  const [activeInventaire, setActiveInventaire] = useState<Inventaire | null>(null);

  // Form State
  const [selectedDepotId, setSelectedDepotId] = useState<string>("");
  const [notes, setNotes] = useState("");
  const [editingLines, setEditingLines] = useState<LigneInventaire[]>([]);
  const { user } = useAuth();

  function loadData() {
    setLoading(true);
    Promise.all([inventairesApi.getAllInventaires(), depotsApi.getAllDepots()])
      .then(([invs, deps]) => {
        setInventaires(invs || []);
        setDepots(deps || []);
      })
      .catch((err) => {
        console.error(err);
        toast.error("Erreur lors du chargement des inventaires");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleStartInventaire() {
    if (!selectedDepotId) {
      toast.error("Veuillez sélectionner un dépôt");
      return;
    }

    try {
      const inv = await inventairesApi.demarrerInventaire(parseInt(selectedDepotId), notes, user?.id);
      toast.success("Session d'inventaire démarrée avec succès !");
      setIsStartModalOpen(false);
      setNotes("");
      loadData();
      openCountingModal(inv);
    } catch (err: any) {
      toast.error("Erreur démarrage inventaire : " + err.message);
    }
  }

  function openCountingModal(inv: Inventaire) {
    setActiveInventaire(inv);
    setEditingLines(inv.lignes ? JSON.parse(JSON.stringify(inv.lignes)) : []);
    setIsCountModalOpen(true);
  }

  function handleQuantityChange(idx: number, val: number) {
    setEditingLines((prev) => {
      const copy = [...prev];
      const qteReelle = val;
      const qteTheo = copy[idx].quantiteTheorique || 0;
      const ecart = qteReelle - qteTheo;
      const prix = copy[idx].prixUnitaire || 0;
      copy[idx] = {
        ...copy[idx],
        quantiteReelle: qteReelle,
        ecart: ecart,
        valeurEcart: ecart * prix,
      };
      return copy;
    });
  }

  async function handleSaveComptage() {
    if (!activeInventaire?.id) return;
    try {
      const updated = await inventairesApi.mettreAJourLignes(activeInventaire.id, editingLines);
      toast.success("Comptage sauvegardé !");
      setActiveInventaire(updated);
      loadData();
    } catch (err: any) {
      toast.error("Erreur enregistrement : " + err.message);
    }
  }

  async function handleValiderInventaire() {
    if (!activeInventaire?.id) return;
    try {
      await inventairesApi.validerInventaire(activeInventaire.id, user?.id);
      toast.success("Inventaire validé ! Les stocks réels ont été ajustés.");
      setIsCountModalOpen(false);
      loadData();
    } catch (err: any) {
      toast.error("Erreur validation inventaire : " + err.message);
    }
  }

  // KPIs
  const totalSessions = inventaires.length;
  const sessionsEnCours = inventaires.filter((i) => i.statut === StatutInventaire.EN_COURS).length;
  const dernierInventaire = inventaires[0];

  const columns: Column<Inventaire>[] = [
    {
      header: "Référence",
      accessor: "reference",
      cell: (row) => (
        <div className="flex items-center gap-2">
          <Boxes className="h-4 w-4 text-primary" />
          <span className="font-semibold text-foreground">{row.reference}</span>
        </div>
      ),
    },
    {
      header: "Date",
      accessor: "dateInventaire",
      cell: (row) => formatDate(row.dateInventaire),
    },
    {
      header: "Dépôt",
      accessor: "depot",
      cell: (row) => (
        <div className="flex items-center gap-1.5">
          <Warehouse className="h-3.5 w-3.5 text-muted-foreground" />
          <span>{row.depot?.nom || "Dépôt Principal"}</span>
        </div>
      ),
    },
    {
      header: "Écart Positif (+)",
      accessor: "totalEcartPositif",
      cell: (row) => (
        <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">
          +{row.totalEcartPositif || 0}
        </span>
      ),
    },
    {
      header: "Écart Négatif (-)",
      accessor: "totalEcartNegatif",
      cell: (row) => (
        <span className="text-xs font-semibold text-red-600 dark:text-red-400">
          -{row.totalEcartNegatif || 0}
        </span>
      ),
    },
    {
      header: "Valeur Écart (MAD)",
      accessor: "valeurTotaleEcart",
      cell: (row) => {
        const val = row.valeurTotaleEcart || 0;
        const color = val > 0 ? "text-emerald-600" : val < 0 ? "text-red-600" : "text-muted-foreground";
        return <span className={`font-bold ${color}`}>{formatMAD(val)}</span>;
      },
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
        <div className="flex justify-end gap-1">
          {row.statut === StatutInventaire.EN_COURS ? (
            <Button size="sm" variant="outline" className="h-7 text-xs" onClick={() => openCountingModal(row)}>
              <Play className="mr-1 h-3.5 w-3.5 text-blue-500" />
              Continuer le comptage
            </Button>
          ) : (
            <Button size="sm" variant="ghost" className="h-7 text-xs" onClick={() => openCountingModal(row)}>
              Consulter
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inventaires Physiques"
        description="Réalisez vos sessions de comptage physique, comparez aux stocks théoriques et ajustez automatiquement vos dépôts."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RotateCcw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button size="sm" onClick={() => setIsStartModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Démarrer un Inventaire
          </Button>
        </div>
      </PageHeader>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard
          title="Total Sessions"
          value={totalSessions}
          description="Historique des comptages"
          icon={Boxes}
          color="blue"
        />
        <StatCard
          title="Sessions en cours"
          value={sessionsEnCours}
          description="Comptage non validé"
          icon={AlertTriangle}
          color="orange"
        />
        <StatCard
          title="Dernière Session"
          value={dernierInventaire?.reference || "Aucune"}
          description={dernierInventaire ? formatDate(dernierInventaire.dateInventaire) : "N/A"}
          icon={Calendar}
          color="green"
        />
      </div>

      <DataTable
        columns={columns}
        data={inventaires}
        loading={loading}
        searchKey="reference"
        searchPlaceholder="Rechercher par référence d'inventaire..."
      />

      {/* Modal Démarrer Inventaire */}
      <Dialog open={isStartModalOpen} onOpenChange={setIsStartModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Boxes className="h-5 w-5 text-primary" />
              Lancer une Nouvelle Session d'Inventaire
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="depot">Dépôt à inventorier *</Label>
              <select
                id="depot"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                value={selectedDepotId}
                onChange={(e) => setSelectedDepotId(e.target.value)}
              >
                <option value="">-- Choisir le dépôt --</option>
                {depots.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nom} ({d.adresse})
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="notes">Remarques / Équipe</Label>
              <Input
                id="notes"
                placeholder="Ex: Inventaire annuel fin d'exercice, équipe magasin..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsStartModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleStartInventaire}>
              Créer et démarrer le comptage
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Modal Saisie du Comptage Réel */}
      <Dialog open={isCountModalOpen} onOpenChange={setIsCountModalOpen}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Boxes className="h-5 w-5 text-primary" />
                <span>Comptage Inventaire : {activeInventaire?.reference}</span>
              </div>
              <StatusBadge status={activeInventaire?.statut as any} />
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="rounded-md border overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs font-semibold text-muted-foreground uppercase border-b">
                  <tr>
                    <th className="px-3 py-2 text-left">Article</th>
                    <th className="px-3 py-2 text-right w-28">Stock Système</th>
                    <th className="px-3 py-2 text-right w-32">Comptage Réel</th>
                    <th className="px-3 py-2 text-right w-24">Écart</th>
                    <th className="px-3 py-2 text-right w-28">P.U Achat</th>
                    <th className="px-3 py-2 text-right w-32">Valeur Écart</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {editingLines.map((line, idx) => {
                    const ecart = line.ecart || 0;
                    const isValide = activeInventaire?.statut === StatutInventaire.VALIDE;
                    return (
                      <tr key={idx} className={ecart !== 0 ? "bg-amber-50/40 dark:bg-amber-950/20" : ""}>
                        <td className="p-3">
                          <div className="font-medium text-foreground">{line.produit?.designation}</div>
                          <div className="text-xs text-muted-foreground">{line.produit?.reference}</div>
                        </td>
                        <td className="p-3 text-right font-medium">
                          {line.quantiteTheorique}
                        </td>
                        <td className="p-3">
                          <Input
                            type="number"
                            disabled={isValide}
                            className="h-8 text-right font-semibold"
                            value={line.quantiteReelle}
                            onChange={(e) => handleQuantityChange(idx, parseFloat(e.target.value) || 0)}
                          />
                        </td>
                        <td className="p-3 text-right">
                          <span
                            className={`text-xs font-bold px-1.5 py-0.5 rounded ${
                              ecart > 0
                                ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                                : ecart < 0
                                ? "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300"
                                : "text-muted-foreground"
                            }`}
                          >
                            {ecart > 0 ? `+${ecart}` : ecart}
                          </span>
                        </td>
                        <td className="p-3 text-right text-muted-foreground text-xs">
                          {formatMAD(line.prixUnitaire)}
                        </td>
                        <td className="p-3 text-right font-semibold text-xs">
                          {formatMAD(line.valeurEcart)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <DialogFooter className="flex items-center justify-between sm:justify-between">
            <Button variant="outline" onClick={() => setIsCountModalOpen(false)}>
              Fermer
            </Button>
            {activeInventaire?.statut === StatutInventaire.EN_COURS && (
              <div className="flex gap-2">
                <Button variant="secondary" onClick={handleSaveComptage}>
                  <Save className="mr-1 h-4 w-4" />
                  Sauvegarder le comptage
                </Button>
                <Button onClick={handleValiderInventaire} className="bg-emerald-600 hover:bg-emerald-700">
                  <CheckCircle2 className="mr-1 h-4 w-4" />
                  Valider & Ajuster le Stock
                </Button>
              </div>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
