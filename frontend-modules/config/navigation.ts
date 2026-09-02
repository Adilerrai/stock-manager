import { NavItem } from "@/types/navigation";
import {
  LayoutDashboard,
  Package,
  Users,
  Truck,
  ShoppingCart,
  Warehouse,
  PackageCheck,
  FileText,
  CreditCard,
  ShoppingBag,
  Landmark,
  Boxes,
  FileMinus,
} from "lucide-react";

export const navigation: NavItem[] = [
  {
    title: "Tableau de bord",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    title: "Produits",
    href: "/produits",
    icon: Package,
    moduleCode: "stock",
  },
  {
    title: "Clients",
    href: "/clients",
    icon: Users,
    moduleCode: "crm",
  },
  {
    title: "Fournisseurs",
    href: "/fournisseurs",
    icon: Truck,
    moduleCode: "achats",
  },
  {
    title: "Devis Clients",
    href: "/ventes/devis",
    icon: FileText,
    moduleCode: "ventes",
  },
  {
    title: "Commandes",
    href: "/commandes",
    icon: ShoppingCart,
    children: [
      {
        title: "Fournisseur",
        href: "/commandes/fournisseur",
        moduleCode: "achats",
      },
      {
        title: "Client",
        href: "/commandes/client",
        moduleCode: "ventes",
      },
    ],
  },
  {
    title: "Stocks",
    href: "/stocks",
    icon: Warehouse,
    moduleCode: "stock",
    children: [
      {
        title: "Vue d'ensemble",
        href: "/stocks",
      },
      {
        title: "Mouvements",
        href: "/stocks/mouvements",
      },
      {
        title: "Inventaires",
        href: "/stocks/inventaires",
      },
      {
        title: "Catégories",
        href: "/stocks/categories",
      },
      {
        title: "Dépôts",
        href: "/stocks/depots",
      },
    ],
  },
  {
    title: "Livraisons",
    href: "/livraisons",
    icon: PackageCheck,
    children: [
      {
        title: "Réception",
        href: "/livraisons/reception",
        moduleCode: "achats",
      },
      {
        title: "Expédition",
        href: "/livraisons/expedition",
        moduleCode: "ventes",
      },
    ],
  },
  {
    title: "Factures & Avoirs",
    href: "/factures",
    icon: FileText,
    children: [
      {
        title: "Factures Ventes",
        href: "/factures/ventes",
        moduleCode: "ventes",
      },
      {
        title: "Factures Achats",
        href: "/factures/achats",
        moduleCode: "achats",
      },
      {
        title: "Avoirs (Retours)",
        href: "/factures/avoirs",
        moduleCode: "ventes",
      },
    ],
  },
  {
    title: "Trésorerie",
    href: "/tresorerie",
    icon: Landmark,
    moduleCode: "tresorerie",
    children: [
      {
        title: "Paiements",
        href: "/paiements",
      },
      {
        title: "Échéancier",
        href: "/tresorerie/echeancier",
      },
      {
        title: "Balance Âgée",
        href: "/tresorerie/balance-agee",
      },
      {
        title: "Relevé Client",
        href: "/tresorerie/releve-client",
      },
      {
        title: "Remises Bancaires",
        href: "/tresorerie/remises",
      },
    ],
  },
  {
    title: "Point de Vente (POS)",
    href: "/ventes",
    icon: ShoppingBag,
    moduleCode: "ventes",
    children: [
      {
        title: "Caisse Comptoir",
        href: "/ventes",
        moduleCode: "ventes",
      },
      {
        title: "Sessions & Journal",
        href: "/ventes/caisses",
        moduleCode: "ventes",
      },
    ],
  },
];
