"use client";

import { useEffect, useState } from "react";
import {
  Landmark,
  Plus,
  CheckCircle2,
  AlertCircle,
  FileText,
  Printer,
  RefreshCw,
  Send,
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
import { tresorerieApi } from "@/lib/api/tresorerie";
import { toast } from "sonner";
import { StatutRemise, type BordereauRemise } from "@/types/commercial";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function RemisesPage() {
  const [remises, setRemises] = useState<BordereauRemise[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form State
  const [nomBanque, setNomBanque] = useState("Attijariwafa Bank");
  const [compteBancaire, setCompteBancaire] = useState("");
  const [typeValeur, setTypeValeur] = useState("CHEQUE");
  const [montantTotal, setMontantTotal] = useState("");
  const [nombreValeurs, setNombreValeurs] = useState("1");
  const [notes, setNotes] = useState("");

  function loadData() {
    setLoading(true);
    tresorerieApi
      .getAllBordereaux()
      .then((data) => setRemises(data || []))
      .catch((err) => {
        console.error(err);
        toast.error("Erreur chargement des remises");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateRemise() {
    if (!nomBanque || !montantTotal) {
      toast.error("Veuillez renseigner la banque et le montant");
      return;
    }

    try {
      await tresorerieApi.creerBordereau({
        nomBanque,
        compteBancaire,
        typeValeur,
        montantTotal: parseFloat(montantTotal),
        nombreValeurs: parseInt(nombreValeurs) || 1,
        notes,
      });
      toast.success("Bordereau de remise créé !");
      setIsModalOpen(false);
      setMontantTotal("");
      setNotes("");
      loadData();
    } catch (err: any) {
      toast.error("Erreur création remise : " + err.message);
    }
  }

  async function handleChangeStatut(id: number, statut: StatutRemise) {
    try {
      await tresorerieApi.changerStatutRemise(id, statut);
      toast.success("Statut de la remise mis à jour !");
      loadData();
    } catch (err: any) {
      toast.error("Erreur mise à jour : " + err.message);
    }
  }

  const columns: Column<BordereauRemise>[] = [
    {
      header: "N° Bordereau",
      accessor: "numeroBordereau",
      cell: (row) => (
        <div className="flex items-center gap-2 font-mono font-semibold text-foreground">
          <Landmark className="h-4 w-4 text-primary" />
          {row.numeroBordereau}
        </div>
      ),
    },
    {
      header: "Date Remise",
      accessor: "dateRemise",
      cell: (row) => formatDate(row.dateRemise),
    },
    {
      header: "Banque Dépositaire",
      accessor: "nomBanque",
      cell: (row) => (
        <div>
          <div className="font-semibold text-foreground">{row.nomBanque}</div>
          {row.compteBancaire && (
            <div className="text-xs text-muted-foreground">RIB : {row.compteBancaire}</div>
          )}
        </div>
      ),
    },
    {
      header: "Type",
      accessor: "typeValeur",
      cell: (row) => (
        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-muted">
          {row.typeValeur === "CHEQUE" ? "Chèques" : "Effets / LCN"}
        </span>
      ),
    },
    {
      header: "Nombre",
      accessor: "nombreValeurs",
      cell: (row) => <span className="font-medium text-xs">{row.nombreValeurs} valeur(s)</span>,
    },
    {
      header: "Montant Total",
      accessor: "montantTotal",
      cell: (row) => <span className="font-bold text-foreground">{formatMAD(row.montantTotal)}</span>,
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
        <div className="flex items-center justify-end gap-1">
          {row.statut === StatutRemise.BROUILLON && (
            <Button
              variant="outline"
              size="sm"
              className="h-7 text-xs"
              onClick={() => row.id && handleChangeStatut(row.id, StatutRemise.REMIS_EN_BANQUE)}
            >
              <Send className="mr-1 h-3.5 w-3.5 text-blue-500" />
              Remettre
            </Button>
          )}
          {row.statut === StatutRemise.REMIS_EN_BANQUE && (
            <Button
              variant="outline"
              size="sm"
              className="h-7 text-xs text-emerald-600 border-emerald-300"
              onClick={() => row.id && handleChangeStatut(row.id, StatutRemise.ENCAISSE)}
            >
              <CheckCircle2 className="mr-1 h-3.5 w-3.5" />
              Encaissé
            </Button>
          )}
          <Button variant="ghost" size="sm" className="h-7 px-2" onClick={() => window.print()}>
            <Printer className="h-4 w-4" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Remises Chèques & Effets en Banque"
        description="Gérez vos bordereaux de dépôt bancaire pour les chèques et les effets de commerce (LCN)."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button size="sm" onClick={() => setIsModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Nouveau Bordereau
          </Button>
        </div>
      </PageHeader>

      <DataTable
        columns={columns}
        data={remises}
        loading={loading}
        searchKey="numeroBordereau"
        searchPlaceholder="Rechercher par numéro de bordereau..."
      />

      {/* Modal Création Bordereau */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Landmark className="h-5 w-5 text-primary" />
              Créer un Bordereau de Remise
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="banque">Banque Dépositaire *</Label>
              <select
                id="banque"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                value={nomBanque}
                onChange={(e) => setNomBanque(e.target.value)}
              >
                <option value="Attijariwafa Bank">Attijariwafa Bank</option>
                <option value="Banque Populaire">Banque Populaire</option>
                <option value="Bank of Africa (BMCE)">Bank of Africa (BMCE)</option>
                <option value="Société Générale Maroc">Société Générale Maroc</option>
                <option value="BMCI">BMCI</option>
                <option value="Crédit Agricole du Maroc">Crédit Agricole du Maroc</option>
                <option value="CIH Bank">CIH Bank</option>
              </select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="rib">N° Compte / RIB</Label>
              <Input
                id="rib"
                placeholder="24 chiffres..."
                value={compteBancaire}
                onChange={(e) => setCompteBancaire(e.target.value)}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label htmlFor="valeur">Type de Valeur</Label>
                <select
                  id="valeur"
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  value={typeValeur}
                  onChange={(e) => setTypeValeur(e.target.value)}
                >
                  <option value="CHEQUE">Chèques</option>
                  <option value="EFFET">Effets / LCN</option>
                </select>
              </div>

              <div className="space-y-1">
                <Label htmlFor="qte">Nombre de titres</Label>
                <Input
                  id="qte"
                  type="number"
                  min="1"
                  value={nombreValeurs}
                  onChange={(e) => setNombreValeurs(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label htmlFor="montant">Montant Total Remis (MAD) *</Label>
              <Input
                id="montant"
                type="number"
                step="0.01"
                placeholder="0.00"
                value={montantTotal}
                onChange={(e) => setMontantTotal(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="notes">Notes</Label>
              <Input
                id="notes"
                placeholder="Références des chèques..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleCreateRemise}>
              Enregistrer le bordereau
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
