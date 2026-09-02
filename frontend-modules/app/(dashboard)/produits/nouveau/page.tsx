"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { produitsApi } from "@/lib/api/produits";
import { categoriesApi, type CategorieDTO } from "@/lib/api/categories";
import { Loader2, Save, Plus } from "lucide-react";
import type { ProduitDTO } from "@/types/api";
import { UniteMesure } from "@/types/api";

export default function NouveauProduitPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<CategorieDTO[]>([]);

  const [form, setForm] = useState<ProduitDTO>({
    reference: "",
    designation: "",
    description: "",
    categorieArticle: undefined,
    groupeArticle: undefined,
    uniteMesureStock: UniteMesure.PIECE,
    prixAchatHt: 0,
    prixAchatTtc: 0,
    prixVenteHt: 0,
    prixVenteTtc: 0,
    codeBarre: "",
    actif: true,
  });

  useEffect(() => {
    categoriesApi
      .getAllCategories()
      .then((data) => setCategories(data || []))
      .catch(console.error);
  }, []);

  function updateField(key: keyof ProduitDTO, value: unknown) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const created = await produitsApi.create(form);
      router.push(`/produits/${created.id}`);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Nouveau produit" description="Ajouter un produit au catalogue." />

      <form onSubmit={handleSubmit}>
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Informations générales */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Informations générales</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="reference">Référence (générée auto si vide)</Label>
                  <Input
                    id="reference"
                    value={form.reference}
                    onChange={(e) => updateField("reference", e.target.value)}
                    placeholder="Ex: REF-001"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="codeBarre">Code barre</Label>
                  <Input
                    id="codeBarre"
                    value={form.codeBarre}
                    onChange={(e) => updateField("codeBarre", e.target.value)}
                    placeholder="Ex: 619..."
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="designation">Désignation *</Label>
                <Input
                  id="designation"
                  value={form.designation}
                  onChange={(e) => updateField("designation", e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={form.description}
                  onChange={(e) => updateField("description", e.target.value)}
                  rows={3}
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label>Catégorie</Label>
                    <Link
                      href="/stocks/categories"
                      target="_blank"
                      className="text-xs text-primary hover:underline flex items-center gap-0.5"
                    >
                      <Plus className="h-3 w-3" /> Gérer
                    </Link>
                  </div>
                  <Select
                    value={form.categorieArticle}
                    onValueChange={(v) => {
                      const selected = categories.find((c) => c.nom === v);
                      setForm((prev) => ({
                        ...prev,
                        categorieArticle: v as any,
                        categorieId: selected?.id,
                      }));
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Sélectionner une catégorie" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories.map((cat) => (
                        <SelectItem key={cat.id || cat.nom} value={cat.nom}>
                          <div className="flex items-center gap-2">
                            {cat.couleur && (
                              <span
                                className="h-2.5 w-2.5 rounded-full flex-shrink-0"
                                style={{ backgroundColor: cat.couleur }}
                              />
                            )}
                            <span>{cat.nom}</span>
                          </div>
                        </SelectItem>
                      ))}
                      {!categories.length && (
                        <SelectItem value="GÉNÉRAL">Général (Aucune catégorie configurée)</SelectItem>
                      )}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="groupe">Groupe / Sous-catégorie</Label>
                  <Input
                    id="groupe"
                    value={form.groupeArticle as any || ""}
                    onChange={(e) => updateField("groupeArticle", e.target.value)}
                    placeholder="Ex: Électroménager, Accessoires..."
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label>Unité de mesure</Label>
                <Select
                  value={form.uniteMesureStock}
                  onValueChange={(v) => updateField("uniteMesureStock", v)}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {Object.values(UniteMesure).map((u) => (
                      <SelectItem key={u} value={u}>
                        {u}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          {/* Tarification */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Tarification</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="prixAchatHt">Prix achat HT</Label>
                  <Input
                    id="prixAchatHt"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.prixAchatHt ?? ""}
                    onChange={(e) => updateField("prixAchatHt", parseFloat(e.target.value) || 0)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="prixAchatTtc">Prix achat TTC</Label>
                  <Input
                    id="prixAchatTtc"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.prixAchatTtc ?? ""}
                    onChange={(e) => updateField("prixAchatTtc", parseFloat(e.target.value) || 0)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="prixVenteHt">Prix vente HT</Label>
                  <Input
                    id="prixVenteHt"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.prixVenteHt ?? ""}
                    onChange={(e) => updateField("prixVenteHt", parseFloat(e.target.value) || 0)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="prixVenteTtc">Prix vente TTC</Label>
                  <Input
                    id="prixVenteTtc"
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.prixVenteTtc ?? ""}
                    onChange={(e) => updateField("prixVenteTtc", parseFloat(e.target.value) || 0)}
                  />
                </div>
              </CardContent>
            </Card>

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Enregistrement...
                </>
              ) : (
                <>
                  <Save className="mr-2 h-4 w-4" />
                  Créer le produit
                </>
              )}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}
