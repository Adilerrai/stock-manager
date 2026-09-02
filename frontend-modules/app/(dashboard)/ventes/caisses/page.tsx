"use client";

import { useEffect, useState } from "react";
import {
  Coins,
  Lock,
  Unlock,
  AlertTriangle,
  CheckCircle2,
  DollarSign,
  CreditCard,
  Banknote,
  FileCheck,
  RefreshCw,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { DataTable, Column } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { caissesApi } from "@/lib/api/caisses";
import { useAuth } from "@/providers/auth-provider";
import { toast } from "sonner";
import { StatutSessionCaisse, type SessionCaisse } from "@/types/commercial";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function CaissesPage() {
  const [sessions, setSessions] = useState<SessionCaisse[]>([]);
  const [activeSession, setActiveSession] = useState<SessionCaisse | null>(null);
  const [loading, setLoading] = useState(true);
  const [isOpenModalOpen, setIsOpenModalOpen] = useState(false);
  const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);

  // Form State
  const [fondInitial, setFondInitial] = useState("500");
  const [montantReelCloture, setMontantReelCloture] = useState("");
  const [notes, setNotes] = useState("");
  const { user } = useAuth();

  function loadData() {
    setLoading(true);
    caissesApi
      .getAllSessions()
      .then((data) => {
        setSessions(data || []);
        if (user?.id) {
          caissesApi.getSessionActive(user.id).then(setActiveSession).catch(console.error);
        }
      })
      .catch((err) => {
        console.error(err);
        toast.error("Erreur chargement des sessions de caisse");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, [user?.id]);

  async function handleOuvrirCaisse() {
    if (!user?.id) return;
    try {
      const sess = await caissesApi.ouvrirSession(user.id, parseFloat(fondInitial) || 0, notes);
      toast.success("Session de caisse ouverte !");
      setActiveSession(sess);
      setIsOpenModalOpen(false);
      loadData();
    } catch (err: any) {
      toast.error("Erreur ouverture caisse : " + err.message);
    }
  }

  async function handleCloturerCaisse() {
    if (!activeSession?.id) return;
    try {
      await caissesApi.cloturerSession(activeSession.id, parseFloat(montantReelCloture) || 0, notes);
      toast.success("Caisse clôturée avec succès !");
      setIsCloseModalOpen(false);
      setActiveSession(null);
      loadData();
    } catch (err: any) {
      toast.error("Erreur clôture caisse : " + err.message);
    }
  }

  const columns: Column<SessionCaisse>[] = [
    {
      header: "Référence",
      accessor: "reference",
      cell: (row) => (
        <div className="flex items-center gap-2 font-mono font-semibold text-foreground">
          <Coins className="h-4 w-4 text-primary" />
          {row.reference}
        </div>
      ),
    },
    {
      header: "Ouverture",
      accessor: "dateOuverture",
      cell: (row) => formatDate(row.dateOuverture),
    },
    {
      header: "Clôture",
      accessor: "dateCloture",
      cell: (row) => formatDate(row.dateCloture),
    },
    {
      header: "Fond Initial",
      accessor: "fondDeCaisseInitial",
      cell: (row) => formatMAD(row.fondDeCaisseInitial),
    },
    {
      header: "Total Ventes",
      accessor: "totalVentes",
      cell: (row) => <span className="font-bold text-foreground">{formatMAD(row.totalVentes)}</span>,
    },
    {
      header: "Espèces Réelles",
      accessor: "montantReelCloture",
      cell: (row) => (row.montantReelCloture != null ? formatMAD(row.montantReelCloture) : "—"),
    },
    {
      header: "Écart de Caisse",
      accessor: "ecartCaisse",
      cell: (row) => {
        if (row.ecartCaisse == null) return "—";
        const ecart = row.ecartCaisse;
        const color =
          ecart > 0
            ? "text-emerald-600 font-bold"
            : ecart < 0
            ? "text-red-600 font-bold"
            : "text-muted-foreground";
        return <span className={color}>{formatMAD(ecart)}</span>;
      },
    },
    {
      header: "Statut",
      accessor: "statut",
      cell: (row) => <StatusBadge status={row.statut as any} />,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Journal & Sessions de Caisse"
        description="Contrôlez les ouvertures/clôtures de caisse comptoir, les fonds de roulement et les écarts d'espèces."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          {!activeSession ? (
            <Button size="sm" onClick={() => setIsOpenModalOpen(true)} className="bg-emerald-600 hover:bg-emerald-700">
              <Unlock className="mr-2 h-4 w-4" />
              Ouvrir la Caisse
            </Button>
          ) : (
            <Button size="sm" variant="destructive" onClick={() => setIsCloseModalOpen(true)}>
              <Lock className="mr-2 h-4 w-4" />
              Clôturer la Caisse
            </Button>
          )}
        </div>
      </PageHeader>

      {/* Active Session Highlight Banner */}
      {activeSession && (
        <Card className="border-emerald-500/30 bg-emerald-500/5">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
                </span>
                <CardTitle className="text-lg font-bold text-foreground">
                  Session de Caisse en Cours : {activeSession.reference}
                </CardTitle>
              </div>
              <span className="text-xs text-muted-foreground">
                Ouverte le {formatDate(activeSession.dateOuverture)}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6 pt-2">
              <div>
                <div className="text-xs text-muted-foreground">Fond Initial</div>
                <div className="text-lg font-bold">{formatMAD(activeSession.fondDeCaisseInitial)}</div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Espèces Encaissées</div>
                <div className="text-lg font-bold text-emerald-600">
                  {formatMAD(activeSession.totalEspeces)}
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Cartes Bancaires</div>
                <div className="text-lg font-bold text-blue-600">
                  {formatMAD(activeSession.totalCarte)}
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Chèques</div>
                <div className="text-lg font-bold text-purple-600">
                  {formatMAD(activeSession.totalCheque)}
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Virements & Crédits</div>
                <div className="text-lg font-bold text-orange-600">
                  {formatMAD((activeSession.totalVirement || 0) + (activeSession.totalCredit || 0))}
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Total Encaissé</div>
                <div className="text-lg font-extrabold text-foreground">
                  {formatMAD(activeSession.totalVentes)}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Table des sessions */}
      <DataTable
        columns={columns}
        data={sessions}
        loading={loading}
        searchKey="reference"
        searchPlaceholder="Rechercher par référence de session..."
      />

      {/* Modal Ouverture Caisse */}
      <Dialog open={isOpenModalOpen} onOpenChange={setIsOpenModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Unlock className="h-5 w-5 text-emerald-600" />
              Ouverture de Caisse
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="fond">Fond de caisse initial (MAD) *</Label>
              <Input
                id="fond"
                type="number"
                value={fondInitial}
                onChange={(e) => setFondInitial(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="notesOpen">Notes / Observations</Label>
              <Input
                id="notesOpen"
                placeholder="Ex: Monnaie préparée..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsOpenModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleOuvrirCaisse} className="bg-emerald-600 hover:bg-emerald-700">
              Confirmer l'ouverture
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Modal Clôture Caisse */}
      <Dialog open={isCloseModalOpen} onOpenChange={setIsCloseModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-red-600">
              <Lock className="h-5 w-5" />
              Clôture et Contrôle de Caisse
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="bg-muted p-3 rounded-md text-sm space-y-1">
              <div className="flex justify-between">
                <span>Fond initial :</span>
                <span>{formatMAD(activeSession?.fondDeCaisseInitial)}</span>
              </div>
              <div className="flex justify-between">
                <span>Espèces encaissées :</span>
                <span>{formatMAD(activeSession?.totalEspeces)}</span>
              </div>
              <div className="flex justify-between font-bold border-t pt-1">
                <span>Espèces théoriques attendues :</span>
                <span>{formatMAD(activeSession?.montantTheoriqueCloture)}</span>
              </div>
            </div>

            <div className="space-y-1">
              <Label htmlFor="reel">Montant réel compté dans le tiroir (MAD) *</Label>
              <Input
                id="reel"
                type="number"
                placeholder="Saisir le comptage physique des espèces"
                value={montantReelCloture}
                onChange={(e) => setMontantReelCloture(e.target.value)}
              />
            </div>

            {montantReelCloture && (
              <div className="text-xs text-muted-foreground p-2 rounded border">
                Écart prévisionnel :{" "}
                <span className="font-bold">
                  {formatMAD(
                    parseFloat(montantReelCloture) - (activeSession?.montantTheoriqueCloture || 0)
                  )}
                </span>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCloseModalOpen(false)}>
              Annuler
            </Button>
            <Button variant="destructive" onClick={handleCloturerCaisse}>
              Valider la clôture
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
