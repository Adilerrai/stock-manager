"use client";

import { useEffect, useState } from "react";
import {
  FileText,
  User,
  Printer,
  Calendar,
  DollarSign,
  TrendingDown,
  TrendingUp,
  CreditCard,
  Search,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { tresorerieApi } from "@/lib/api/tresorerie";
import { clientsApi } from "@/lib/api/clients";
import { toast } from "sonner";
import type { ReleveClient } from "@/types/commercial";
import type { Client } from "@/types/api";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function ReleveClientPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientId, setSelectedClientId] = useState<string>("");
  const [dateDebut, setDateDebut] = useState<string>("");
  const [dateFin, setDateFin] = useState<string>("");
  const [releve, setReleve] = useState<ReleveClient | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    clientsApi.getAllClients().then((data) => {
      setClients(data || []);
      if (data && data.length > 0) {
        setSelectedClientId(data[0].id.toString());
      }
    });
  }, []);

  useEffect(() => {
    if (selectedClientId) {
      handleFetchReleve();
    }
  }, [selectedClientId]);

  async function handleFetchReleve() {
    if (!selectedClientId) return;
    setLoading(true);
    try {
      const data = await tresorerieApi.getReleveClient(
        parseInt(selectedClientId),
        dateDebut || undefined,
        dateFin || undefined
      );
      setReleve(data);
    } catch (err: any) {
      console.error(err);
      toast.error("Erreur lors de la génération du relevé client");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Relevé de Compte Client"
        description="Générez un extrait chronologique consolidé des factures, règlements et avoirs d'un client avec solde progressif."
      >
        <Button variant="outline" size="sm" onClick={() => window.print()}>
          <Printer className="mr-2 h-4 w-4" />
          Imprimer le Relevé
        </Button>
      </PageHeader>

      {/* Selecteurs et Filtres */}
      <Card>
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-4 items-end">
            <div className="space-y-1 md:col-span-2">
              <Label htmlFor="clientSelect">Sélectionner le Client</Label>
              <select
                id="clientSelect"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                value={selectedClientId}
                onChange={(e) => setSelectedClientId(e.target.value)}
              >
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nomComplet || c.nom} ({c.telephone || c.email || "Sans coordonnées"})
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <Label htmlFor="dateDebut">Date Début</Label>
              <Input
                id="dateDebut"
                type="date"
                value={dateDebut}
                onChange={(e) => setDateDebut(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="dateFin">Date Fin</Label>
              <div className="flex gap-2">
                <Input
                  id="dateFin"
                  type="date"
                  value={dateFin}
                  onChange={(e) => setDateFin(e.target.value)}
                />
                <Button size="icon" onClick={handleFetchReleve}>
                  <Search className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Relevé Content */}
      {releve && (
        <div className="space-y-6">
          {/* Client Header Info */}
          <div className="rounded-xl border bg-card p-6 shadow-sm">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
              <div>
                <span className="text-xs uppercase tracking-wider text-muted-foreground font-semibold">
                  Fiche Client
                </span>
                <h2 className="text-2xl font-bold text-foreground">{releve.clientNom}</h2>
                <div className="flex flex-wrap gap-4 text-xs text-muted-foreground mt-1">
                  {releve.telephone && <span>Tél : {releve.telephone}</span>}
                  {releve.email && <span>Email : {releve.email}</span>}
                  {releve.ice && <span>ICE : {releve.ice}</span>}
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-muted-foreground">Solde Actuel Dû</div>
                <div
                  className={`text-2xl font-extrabold ${
                    releve.soldeActuel > 0 ? "text-red-600" : "text-emerald-600"
                  }`}
                >
                  {formatMAD(releve.soldeActuel)}
                </div>
                <div className="text-xs text-muted-foreground mt-0.5">
                  Crédit autorisé : {formatMAD(releve.creditAutorise)}
                </div>
              </div>
            </div>
          </div>

          {/* KPI Cards */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              title="Total Facturé (Débit)"
              value={formatMAD(releve.totalFactures)}
              description="Ventes comptabilisées"
              icon={TrendingUp}
              color="blue"
            />
            <StatCard
              title="Total Encaissé (Crédit)"
              value={formatMAD(releve.totalPaiements)}
              description="Règlements reçus"
              icon={TrendingDown}
              color="green"
            />
            <StatCard
              title="Total Avoirs Déduits"
              value={formatMAD(releve.totalAvoirs)}
              description="Retours et déductions"
              icon={CreditCard}
              color="orange"
            />
            <StatCard
              title="Reste à Recouvrer"
              value={formatMAD(releve.soldeActuel)}
              description="Solde net du compte"
              icon={DollarSign}
              color={releve.soldeActuel > 0 ? "red" : "green"}
            />
          </div>

          {/* Timeline Table */}
          <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
            <div className="p-4 border-b bg-muted/20 flex items-center justify-between">
              <h3 className="font-semibold text-foreground">Écritures & Mouvements Chronologiques</h3>
              <span className="text-xs text-muted-foreground">
                {releve.operations.length} opérations
              </span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs font-semibold text-muted-foreground uppercase border-b">
                  <tr>
                    <th className="px-4 py-3 text-left">Date</th>
                    <th className="px-4 py-3 text-left">Type</th>
                    <th className="px-4 py-3 text-left">Référence</th>
                    <th className="px-4 py-3 text-left">Libellé de l'Opération</th>
                    <th className="px-4 py-3 text-right">Débit (+)</th>
                    <th className="px-4 py-3 text-right">Crédit (-)</th>
                    <th className="px-4 py-3 text-right">Solde Progressif</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {releve.operations.map((op, idx) => (
                    <tr key={idx} className="hover:bg-muted/30 transition-colors">
                      <td className="px-4 py-3 text-xs">{formatDate(op.date)}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`text-xs font-semibold px-2 py-0.5 rounded ${
                            op.typeOperation === "FACTURE"
                              ? "bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300"
                              : op.typeOperation === "PAIEMENT"
                              ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-300"
                              : "bg-orange-100 text-orange-800 dark:bg-orange-900/50 dark:text-orange-300"
                          }`}
                        >
                          {op.typeOperation}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-medium text-foreground">{op.reference}</td>
                      <td className="px-4 py-3 text-muted-foreground text-xs">{op.libelle}</td>
                      <td className="px-4 py-3 text-right font-semibold text-blue-600">
                        {op.debit > 0 ? formatMAD(op.debit) : "—"}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-emerald-600">
                        {op.credit > 0 ? formatMAD(op.credit) : "—"}
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-foreground">
                        {formatMAD(op.soldeProgressif)}
                      </td>
                    </tr>
                  ))}
                  {!releve.operations.length && (
                    <tr>
                      <td colSpan={7} className="p-6 text-center text-muted-foreground">
                        Aucun mouvement enregistré sur cette période.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
