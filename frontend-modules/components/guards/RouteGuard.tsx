"use client";

import { ReactNode, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useWorkspace, ModuleCode } from "@/lib/context/WorkspaceContext";

// Configuration qui map chaque route principale à un code de module
const routeModuleMap: Record<string, ModuleCode | null> = {
  "/dashboard": null, // Accessible à tous
  "/produits": "stock",
  "/clients": "crm",
  "/fournisseurs": "achats",
  "/commandes/fournisseur": "achats",
  "/commandes/client": "ventes",
  "/stocks": "stock",
  "/livraisons/reception": "achats",
  "/livraisons/expedition": "ventes",
  "/factures/ventes": "ventes",
  "/factures/achats": "achats",
  "/factures/avoirs": "ventes",
  "/factures": "ventes",
  "/paiements": "tresorerie",
  "/tresorerie": "tresorerie",
  "/ventes": "ventes",
};

export function RouteGuard({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { hasModule, isLoading } = useWorkspace();

  useEffect(() => {
    if (isLoading) return;

    // Trouver le module requis pour la route actuelle
    let requiredModule: ModuleCode | null = null;
    
    for (const [route, moduleCode] of Object.entries(routeModuleMap)) {
      if (pathname.startsWith(route) && moduleCode) {
        requiredModule = moduleCode;
        break;
      }
    }

    if (requiredModule && !hasModule(requiredModule)) {
      // Si l'utilisateur n'a pas accès, on le redirige vers le dashboard ou une page d'upgrade
      router.push("/dashboard");
    }
  }, [pathname, hasModule, isLoading, router]);

  if (isLoading) {
    return null; // Ou loader global
  }

  return <>{children}</>;
}
