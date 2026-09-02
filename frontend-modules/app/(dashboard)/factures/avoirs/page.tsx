"use client";

import { useEffect, useState } from "react";
import {
  FileMinus,
  Plus,
  CheckCircle2,
  AlertCircle,
  DollarSign,
  Printer,
  RefreshCw,
  Warehouse,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { DataTable, Column } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { avoirsApi } from "@/lib/api/avoirs";
import { facturesApi } from "@/lib/api/factures";
import { clientsApi } from "@/lib/api/clients";
import { fournisseursApi } from "@/lib/api/fournisseurs";
import { useAuth } from "@/providers/auth-provider";
import { toast } from "sonner";
import { TypeAvoir, StatutAvoir, type Avoir } from "@/types/commercial";
import type { Client, Fournisseur, Facture } from "@/types/api";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function AvoirsPage() {
  const [avoirs, setAvoirs] = useState<Avoir[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentTab, setCurrentTab] = useState<"ALL" | "CLIENT" | "FOURNISSEUR">("ALL");
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form
  const [typeAvoir, setTypeAvoir] = useState<TypeAvoir>(TypeAvoir.CLIENT);
  const [facturesClients, setFacturesClients] = useState<Facture[]>([]);
  const [selectedFactureId, setSelectedFactureId] = useState<string>("");
  const [motif, setMotif] = useState("");
  const { user } = useAuth();

  function loadData() {
    setLoading(true);
    avoirsApi
      .getAllAvoirs()
      .then((data) => setAvoirs(data || []))
      .catch((err) => {
        console.error(err);
        toast.error("Erreur lors du chargement des avoirs");
      })
      .finally(() => setLoading(false));

    facturesApi.getAllFactures().then(setFacturesClients).catch(console.error);
  }

  useEffect(() => {
    loadData();
  }, []);

  const filteredAvoirs = avoirs.filter((a) => {
    if (currentTab === "ALL") return true;
    return a.typeAvoir === currentTab;
  });

  async function handleCreateFromFacture() {
    if (!selectedFactureId) {
      toast.error("Veuillez sélectionner une facture");
      return;
    }

    try {
      await avoirsApi.creerDepuisFacture(parseInt(selectedFactureId), motif, user?.id);
      toast.success("Avoir généré avec succès depuis la facture !");
      setIsModalOpen(false);
      setSelectedFactureId("");
      setMotif("");
      loadData();
    } catch (err: any) {
      toast.error("Erreur création avoir : " + err.message);
    }
  }

  async function handleValiderAvoir(id: number) {
    try {
      await avoirsApi.validerAvoir(id);
      toast.success("Avoir validé et stock réintégré !");
      loadData();
    } catch (err: any) {
      toast.error("Erreur validation avoir : " + err.message);
    }
  }

  // KPIs
  const totalAvoirsCount = avoirs.length;
  const totalMontantTTC = avoirs.reduce((acc, a) => acc + (a.montantTTC || 0), 0);
  const totalClientsTTC = avoirs
    .filter((a) => a.typeAvoir === TypeAvoir.CLIENT)
    .reduce((acc, a) => acc + (a.montantTTC || 0), 0);
  const avoirsEnAttente = avoirs.filter((a) => a.statut === StatutAvoir.BROUILLON).length;

  const columns: Column<Avoir>[] = [
    {
      header: "N° Avoir",
      accessor: "numeroAvoir",
      cell: (row) => (
        <div className="flex items-center gap-2">
          <FileMinus className="h-4 w-4 text-orange-500" />
          <span className="font-semibold text-foreground">{row.numeroAvoir}</span>
        </div>
      ),
    },
    {
      header: "Date",
      accessor: "dateAvoir",
      cell: (row) => formatDate(row.dateAvoir),
    },
    {
      header: "Type",
      accessor: "typeAvoir",
      cell: (row) => (
        <span
          className={`px-2 py-0.5 text-xs font-semibold rounded ${
            row.typeAvoir === TypeAvoir.CLIENT
              ? "bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300"
              : "bg-purple-100 text-purple-800 dark:bg-purple-900/50 dark:text-purple-300"
          }`}
        >
          {row.typeAvoir === TypeAvoir.CLIENT ? "Client" : "Fournisseur"}
        </span>
      ),
    },
    {
      header: "Tiers",
      accessor: "client",
      cell: (row) => (
        <div>
          {row.typeAvoir === TypeAvoir.CLIENT
            ? row.client?.nomComplet || row.client?.nom || "—"
            : row.fournisseur?.raisonSociale || "—"}
        </div>
      ),
    },
    {
      header: "Facture Réf.",
      accessor: "numeroFactureOrigine",
      cell: (row) => (
        <span className="text-xs text-muted-foreground">{row.numeroFactureOrigine || "—"}</span>
      ),
    },
    {
      header: "Montant TTC",
      accessor: "montantTTC",
      cell: (row) => <span className="font-bold text-foreground">{formatMAD(row.montantTTC)}</span>,
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
          {row.statut === StatutAvoir.BROUILLON && (
            <Button
              variant="outline"
              size="sm"
              className="text-xs h-7 text-emerald-600 border-emerald-300 hover:bg-emerald-50 dark:hover:bg-emerald-950/30"
              onClick={() => row.id && handleValiderAvoir(row.id)}
            >
              <CheckCircle2 className="mr-1 h-3.5 w-3.5" />
              Valider & Stock
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
        title="Factures d'Avoir"
        description="Gérez les retours marchandises, annulations et avoirs clients & fournisseurs avec régularisation automatique du stock."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button size="sm" onClick={() => setIsModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Créer un Avoir
          </Button>
        </div>
      </PageHeader>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Avoirs"
          value={totalAvoirsCount}
          description="Tous types confondus"
          icon={FileMinus}
          color="orange"
        />
        <StatCard
          title="Montant Total Avoirs"
          value={formatMAD(totalMontantTTC)}
          description="Cumul TTC"
          icon={DollarSign}
          color="red"
        />
        <StatCard
          title="Avoirs Clients"
          value={formatMAD(totalClientsTTC)}
          description="Remboursements / Déductions"
          icon={AlertCircle}
          color="blue"
        />
        <StatCard
          title="En Attente de Validation"
          value={avoirsEnAttente}
          description="Stock non encore régularisé"
          icon={Warehouse}
          color="indigo"
        />
      </div>

      <div className="flex items-center justify-between border-b pb-2">
        <Tabs value={currentTab} onValueChange={(v) => setCurrentTab(v as any)}>
          <TabsList>
            <TabsTrigger value="ALL">Tous les Avoirs</TabsTrigger>
            <TabsTrigger value="CLIENT">Avoirs Clients</TabsTrigger>
            <TabsTrigger value="FOURNISSEUR">Avoirs Fournisseurs</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <DataTable
        columns={columns}
        data={filteredAvoirs}
        loading={loading}
        searchKey="numeroAvoir"
        searchPlaceholder="Rechercher par numéro d'avoir..."
      />

      {/* Modal Création Avoir depuis Facture */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileMinus className="h-5 w-5 text-orange-500" />
              Générer un Avoir depuis une Facture
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label>Type d'Avoir</Label>
              <div className="grid grid-cols-2 gap-2">
                <Button
                  type="button"
                  variant={typeAvoir === TypeAvoir.CLIENT ? "default" : "outline"}
                  onClick={() => setTypeAvoir(TypeAvoir.CLIENT)}
                >
                  Avoir Client
                </Button>
                <Button
                  type="button"
                  variant={typeAvoir === TypeAvoir.FOURNISSEUR ? "default" : "outline"}
                  onClick={() => setTypeAvoir(TypeAvoir.FOURNISSEUR)}
                >
                  Avoir Fournisseur
                </Button>
              </div>
            </div>

            <div className="space-y-1">
              <Label htmlFor="facture">Facture d'Origine *</Label>
              <select
                id="facture"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                value={selectedFactureId}
                onChange={(e) => setSelectedFactureId(e.target.value)}
              >
                <option value="">-- Sélectionner la facture --</option>
                {facturesClients.map((f) => (
                  <option key={f.id} value={f.id}>
                    N° {f.numeroFacture} - {f.client?.nomComplet || f.client?.nom} ({formatMAD(f.montantFinal)})
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="motif">Motif de l'Avoir</Label>
              <Input
                id="motif"
                placeholder="Ex: Retour marchandise défectueuse, remise commerciale..."
                value={motif}
                onChange={(e) => setMotif(e.target.value)}
              />
            </div>

            <div className="text-xs text-muted-foreground bg-muted p-2.5 rounded border">
              💡 La validation de l'avoir réintégrera automatiquement les articles dans votre inventaire de stock.
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleCreateFromFacture}>
              Générer l'avoir
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
