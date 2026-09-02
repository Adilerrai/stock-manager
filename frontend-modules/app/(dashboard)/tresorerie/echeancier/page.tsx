"use client";

import { useEffect, useState } from "react";
import {
  CalendarClock,
  ArrowDownLeft,
  ArrowUpRight,
  Scale,
  RefreshCw,
  Printer,
  FileText,
  CreditCard,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { tresorerieApi } from "@/lib/api/tresorerie";
import { toast } from "sonner";
import type { Echeancier } from "@/types/commercial";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function EcheancierPage() {
  const [echeancier, setEcheancier] = useState<Echeancier | null>(null);
  const [loading, setLoading] = useState(true);
  const [dateDebut, setDateDebut] = useState("");
  const [dateFin, setDateFin] = useState("");

  function loadData() {
    setLoading(true);
    tresorerieApi
      .getEcheancier(dateDebut || undefined, dateFin || undefined)
      .then(setEcheancier)
      .catch((err) => {
        console.error(err);
        toast.error("Erreur chargement de l'échéancier");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Échéancier de Trésorerie"
        description="Visualisez vos entrées prévisionnelles (factures clients, chèques en portefeuille) et vos sorties attendues (factures achats)."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button variant="outline" size="sm" onClick={() => window.print()}>
            <Printer className="mr-2 h-4 w-4" />
            Imprimer
          </Button>
        </div>
      </PageHeader>

      {/* KPI Cards */}
      {echeancier && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard
            title="Total à Encaisser (+)"
            value={formatMAD(echeancier.totalAEncaisser)}
            description="Créances clients & chèques"
            icon={ArrowDownLeft}
            color="green"
          />
          <StatCard
            title="Total à Décaiser (-)"
            value={formatMAD(echeancier.totalAPayer)}
            description="Dettes fournisseurs"
            icon={ArrowUpRight}
            color="red"
          />
          <StatCard
            title="Solde Net Prévisionnel"
            value={formatMAD(echeancier.soldePrevisionnel)}
            description="Entrées - Sorties"
            icon={Scale}
            color={echeancier.soldePrevisionnel >= 0 ? "blue" : "red"}
          />
        </div>
      )}

      {/* Table */}
      <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
        <div className="p-4 border-b bg-muted/20 flex items-center justify-between">
          <h3 className="font-semibold text-foreground">Calendrier des Échéances</h3>
          <span className="text-xs text-muted-foreground">
            {echeancier?.echeances?.length || 0} échéances identifiées
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-xs font-semibold text-muted-foreground uppercase border-b">
              <tr>
                <th className="px-4 py-3 text-left">Date Échéance</th>
                <th className="px-4 py-3 text-left">Flux</th>
                <th className="px-4 py-3 text-left">Tiers</th>
                <th className="px-4 py-3 text-left">Type de Document</th>
                <th className="px-4 py-3 text-left">Référence</th>
                <th className="px-4 py-3 text-right">Montant</th>
                <th className="px-4 py-3 text-right">Statut</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {echeancier?.echeances?.map((item, idx) => {
                const isEnc = item.sens === "ENCAISSEMENT";
                return (
                  <tr key={idx} className="hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3 font-semibold text-foreground text-xs">
                      {formatDate(item.dateEcheance)}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center gap-1 text-xs font-bold px-2 py-0.5 rounded ${
                          isEnc
                            ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                            : "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300"
                        }`}
                      >
                        {isEnc ? (
                          <>
                            <ArrowDownLeft className="h-3 w-3" /> Entrée
                          </>
                        ) : (
                          <>
                            <ArrowUpRight className="h-3 w-3" /> Sortie
                          </>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-foreground">{item.tiersNom}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{item.typeDocument}</td>
                    <td className="px-4 py-3 font-mono text-xs">{item.reference}</td>
                    <td
                      className={`px-4 py-3 text-right font-bold ${
                        isEnc ? "text-emerald-600" : "text-red-600"
                      }`}
                    >
                      {formatMAD(item.montant)}
                    </td>
                    <td className="px-4 py-3 text-right text-xs text-muted-foreground">
                      {item.statut}
                    </td>
                  </tr>
                );
              })}
              {!echeancier?.echeances?.length && (
                <tr>
                  <td colSpan={7} className="p-6 text-center text-muted-foreground">
                    Aucune échéance à venir.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
