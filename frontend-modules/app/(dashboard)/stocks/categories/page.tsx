"use client";

import { useEffect, useState } from "react";
import {
  Tag,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Palette,
} from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { DataTable, Column } from "@/components/shared/data-table";
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
import { categoriesApi, type CategorieDTO } from "@/lib/api/categories";
import { toast } from "sonner";

const PRESET_COLORS = [
  "#3b82f6", // Blue
  "#10b981", // Emerald
  "#f59e0b", // Amber
  "#ef4444", // Red
  "#8b5cf6", // Purple
  "#ec4899", // Pink
  "#06b6d4", // Cyan
  "#64748b", // Slate
];

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategorieDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCat, setEditingCat] = useState<CategorieDTO | null>(null);

  // Form State
  const [nom, setNom] = useState("");
  const [code, setCode] = useState("");
  const [description, setDescription] = useState("");
  const [couleur, setCouleur] = useState(PRESET_COLORS[0]);
  const [actif, setActif] = useState(true);

  function loadData() {
    setLoading(true);
    categoriesApi
      .getAllCategories()
      .then((data) => setCategories(data || []))
      .catch((err) => {
        console.error(err);
        toast.error("Erreur lors du chargement des catégories");
      })
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    loadData();
  }, []);

  function openCreateModal() {
    setEditingCat(null);
    setNom("");
    setCode("");
    setDescription("");
    setCouleur(PRESET_COLORS[0]);
    setActif(true);
    setIsModalOpen(true);
  }

  function openEditModal(cat: CategorieDTO) {
    setEditingCat(cat);
    setNom(cat.nom);
    setCode(cat.code || "");
    setDescription(cat.description || "");
    setCouleur(cat.couleur || PRESET_COLORS[0]);
    setActif(cat.actif ?? true);
    setIsModalOpen(true);
  }

  async function handleSaveCategory() {
    if (!nom.trim()) {
      toast.error("Le nom de la catégorie est obligatoire");
      return;
    }

    try {
      const payload: Partial<CategorieDTO> = {
        nom: nom.trim(),
        code: code.trim().toUpperCase() || undefined,
        description: description.trim() || undefined,
        couleur,
        actif,
      };

      if (editingCat?.id) {
        await categoriesApi.modifierCategorie(editingCat.id, payload);
        toast.success("Catégorie modifiée avec succès !");
      } else {
        await categoriesApi.creerCategorie(payload);
        toast.success("Catégorie créée avec succès !");
      }

      setIsModalOpen(false);
      loadData();
    } catch (err: any) {
      toast.error("Erreur enregistrement : " + (err.message || ""));
    }
  }

  async function handleDeleteCategory(id: number) {
    if (!confirm("Voulez-vous vraiment supprimer cette catégorie ?")) return;
    try {
      await categoriesApi.supprimerCategorie(id);
      toast.success("Catégorie supprimée !");
      loadData();
    } catch (err: any) {
      toast.error("Erreur suppression : " + err.message);
    }
  }

  const columns: Column<CategorieDTO>[] = [
    {
      header: "Catégorie",
      accessor: "nom",
      cell: (row) => (
        <div className="flex items-center gap-2">
          <span
            className="h-3.5 w-3.5 rounded-full flex-shrink-0"
            style={{ backgroundColor: row.couleur || "#64748b" }}
          />
          <span className="font-semibold text-foreground">{row.nom}</span>
        </div>
      ),
    },
    {
      header: "Code",
      accessor: "code",
      cell: (row) => (
        <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-muted">
          {row.code || "—"}
        </span>
      ),
    },
    {
      header: "Description",
      accessor: "description",
      cell: (row) => <span className="text-muted-foreground text-xs">{row.description || "—"}</span>,
    },
    {
      header: "Statut",
      accessor: "actif",
      cell: (row) =>
        row.actif ? (
          <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400">
            <CheckCircle2 className="h-3.5 w-3.5" /> Actif
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground">
            <XCircle className="h-3.5 w-3.5" /> Inactif
          </span>
        ),
    },
    {
      header: "Actions",
      accessor: "id",
      className: "text-right",
      cell: (row) => (
        <div className="flex items-center justify-end gap-1">
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => openEditModal(row)}
          >
            <Edit2 className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0 text-red-500 hover:text-red-700"
            onClick={() => row.id && handleDeleteCategory(row.id)}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Catégories de Produits"
        description="Gérez les catégories d'articles de votre entreprise selon votre secteur d'activité."
      >
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={loadData}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualiser
          </Button>
          <Button size="sm" onClick={openCreateModal}>
            <Plus className="mr-2 h-4 w-4" />
            Nouvelle Catégorie
          </Button>
        </div>
      </PageHeader>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatCard
          title="Total Catégories"
          value={categories.length}
          description="Créées pour votre entreprise"
          icon={Tag}
          color="blue"
        />
        <StatCard
          title="Catégories Actives"
          value={categories.filter((c) => c.actif).length}
          description="Disponibles pour vos articles"
          icon={CheckCircle2}
          color="green"
        />
      </div>

      <DataTable
        columns={columns}
        data={categories}
        loading={loading}
        searchKey="nom"
        searchPlaceholder="Rechercher une catégorie..."
      />

      {/* Modal Création / Édition */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Tag className="h-5 w-5 text-primary" />
              {editingCat ? "Modifier la Catégorie" : "Créer une Nouvelle Catégorie"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="nomCat">Nom de la catégorie *</Label>
              <Input
                id="nomCat"
                placeholder="Ex: Électronique, Alimentation, Cosmétique..."
                value={nom}
                onChange={(e) => setNom(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="codeCat">Code court / Réf</Label>
              <Input
                id="codeCat"
                placeholder="Ex: ELEC, ALIM, TEXT..."
                value={code}
                onChange={(e) => setCode(e.target.value)}
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="descCat">Description</Label>
              <Input
                id="descCat"
                placeholder="Optionnel..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Sélecteur de Couleur */}
            <div className="space-y-1">
              <Label className="flex items-center gap-1.5">
                <Palette className="h-4 w-4" />
                Couleur d'identification (Badges & Caisse POS)
              </Label>
              <div className="flex items-center gap-2 pt-1">
                {PRESET_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    className={`h-7 w-7 rounded-full border-2 transition-transform ${
                      couleur === c ? "scale-110 border-foreground" : "border-transparent"
                    }`}
                    style={{ backgroundColor: c }}
                    onClick={() => setCouleur(c)}
                  />
                ))}
              </div>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <input
                type="checkbox"
                id="actifCat"
                checked={actif}
                onChange={(e) => setActif(e.target.checked)}
                className="rounded border-input text-primary"
              />
              <Label htmlFor="actifCat" className="cursor-pointer">
                Catégorie active (visible lors de la création d'articles)
              </Label>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Annuler
            </Button>
            <Button onClick={handleSaveCategory}>
              {editingCat ? "Enregistrer les modifications" : "Créer la catégorie"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
