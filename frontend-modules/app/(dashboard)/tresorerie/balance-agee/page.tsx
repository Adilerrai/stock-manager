"use client";

import { useEffect, useState } from "react";
import {
  Clock,
  AlertTriangle,
  CheckCircle2,
  DollarSign,
  TrendingDown,
  Printer,
  RefreshCw,
  Eye,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { tresorerieApi } from "@/lib/api/tresorerie";
import { toast } from "sonner";
import Link from "next/link";
import type { BalanceAgee } from "@/types/commercial";

function formatMAD(v?: number) {
  if (v == null) return "0,00 MAD";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(v);
}

export default function BalanceAgeePage() {
  const [currentTab, setCurrentTab] = useState<"CLIENT" | "FOURNISSEUR">("CLIENT");
  const [balanceClients, setBalanceClients] = useState<BalanceAgee | null>(null);
  const [balanceFournisseurs, setBalanceFournisseurs] = useState<BalanceAgee | null>(null);
  const [loading, setLoading] = useState(true);

  function loadData() {
    setLoading(true);
    Promise.all([
      tresorerieApi.getBalanceAgeeClients(),
      tresorerieApi.getBalanceAgeeFournisseurs(),
    ])
      .then(([cli, frs]) => {
        setBalanceClients(cli);
        setBalanceFournisseurs(frs);
      })
      .catch((err) => {
        console.error(err);
        toast.error("Erreur lors du chargement de la balance âgée");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  const activeBalance = currentTab === "CLIENT" ? balanceClients : balanceFournisseurs;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Balance Âgée"
        description="Analysez l'ancienneté de vos créances clients et de vos dettes fournisseurs réparties par tranche d'échéance."
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

      <Tabs value={currentTab} onValueChange={(v) => setCurrentTab(v as any)}>
        <TabsList>
          <TabsTrigger value="CLIENT">Balance Âgée Clients (Créances)</TabsTrigger>
          <TabsTrigger value="FOURNISSEUR">Balance Âgée Fournisseurs (Dettes)</TabsTrigger>
        </TabsList>
      </Tabs>

      {/* KPI Cards */}
      {activeBalance && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-6">
          <StatCard
            title="Total Dû"
            value={formatMAD(activeBalance.totalCreances)}
            icon={DollarSign}
            color="blue"
          />
          <StatCard
            title="Non Échu"
            value={formatMAD(activeBalance.totalNonEchu)}
            icon={CheckCircle2}
            color="green"
          />
          <StatCard
            title="1 - 30 Jours"
            value={formatMAD(activeBalance.totalMoins30J)}
            icon={Clock}
            color="orange"
          />
          <StatCard
            title="31 - 60 Jours"
            value={formatMAD(activeBalance.total30A60J)}
            icon={Clock}
            color="orange"
          />
          <StatCard
            title="61 - 90 Jours"
            value={formatMAD(activeBalance.total60A90J)}
            icon={AlertTriangle}
            color="red"
          />
          <StatCard
            title="> 90 Jours"
            value={formatMAD(activeBalance.totalPlus90J)}
            icon={AlertTriangle}
            color="red"
          />
        </div>
      )}

      {/* Table */}
      <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
        <div className="p-4 border-b bg-muted/20 flex items-center justify-between">
          <h3 className="font-semibold text-foreground">
            Détail par {currentTab === "CLIENT" ? "Client" : "Fournisseur"}
          </h3>
          <span className="text-xs text-muted-foreground">
            {activeBalance?.tiers?.length || 0} tiers concernés
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-xs font-semibold text-muted-foreground uppercase border-b">
              <tr>
                <th className="px-4 py-3 text-left">Tiers</th>
                <th className="px-4 py-3 text-right">Total Dû</th>
                <th className="px-4 py-3 text-right">Non Échu</th>
                <th className="px-4 py-3 text-right">1 - 30 j</th>
                <th className="px-4 py-3 text-right">31 - 60 j</th>
                <th className="px-4 py-3 text-right">61 - 90 j</th>
                <th className="px-4 py-3 text-right text-red-600">&gt; 90 j (Critique)</th>
                {currentTab === "CLIENT" && <th className="px-4 py-3 text-center">Action</th>}
              </tr>
            </thead>
            <tbody className="divide-y">
              {activeBalance?.tiers?.map((t, idx) => (
                <tr key={idx} className="hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3">
                    <div className="font-semibold text-foreground">{t.tiersNom}</div>
                    {t.telephone && (
                      <div className="text-xs text-muted-foreground">{t.telephone}</div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right font-bold text-foreground">
                    {formatMAD(t.totalDu)}
                  </td>
                  <td className="px-4 py-3 text-right text-emerald-600 font-medium">
                    {t.nonEchu > 0 ? formatMAD(t.nonEchu) : "—"}
                  </td>
                  <td className="px-4 py-3 text-right text-muted-foreground font-medium">
                    {t.moins30J > 0 ? formatMAD(t.moins30J) : "—"}
                  </td>
                  <td className="px-4 py-3 text-right text-amber-600 font-medium">
                    {t.de30A60J > 0 ? formatMAD(t.de30A60J) : "—"}
                  </td>
                  <td className="px-4 py-3 text-right text-orange-600 font-semibold">
                    {t.de60A90J > 0 ? formatMAD(t.de60A90J) : "—"}
                  </td>
                  <td className="px-4 py-3 text-right text-red-600 font-bold bg-red-50/50 dark:bg-red-950/20">
                    {t.plus90J > 0 ? formatMAD(t.plus90J) : "—"}
                  </td>
                  {currentTab === "CLIENT" && (
                    <td className="px-4 py-3 text-center">
                      <Link href={`/tresorerie/releve-client?clientId=${t.tiersId}`}>
                        <Button variant="ghost" size="sm" className="h-7 text-xs">
                          <Eye className="mr-1 h-3.5 w-3.5" />
                          Relevé
                        </Button>
                      </Link>
                    </td>
                  )}
                </tr>
              ))}
              {!activeBalance?.tiers?.length && (
                <tr>
                  <td colSpan={8} className="p-6 text-center text-muted-foreground">
                    Aucune créance ou dette impayée enregistrée.
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
