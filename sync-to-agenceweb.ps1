$source = "frontend-modules"
$dest = "..\agenceweb"

Write-Host "Synchronisation des composants frontend..."

# 1. Types
New-Item -ItemType Directory -Force -Path "$dest\types" | Out-Null
Copy-Item "$source\types\commercial.ts" "$dest\types\commercial.ts" -Force
Write-Host "[OK] types/commercial.ts copié"

# 2. APIs
New-Item -ItemType Directory -Force -Path "$dest\lib\api" | Out-Null
Copy-Item "$source\lib\api\*.ts" "$dest\lib\api\" -Force
Write-Host "[OK] lib/api/* copié"

# 3. Pages App Router
# Devis
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\ventes\devis" | Out-Null
Copy-Item "$source\app\(dashboard)\ventes\devis\page.tsx" "$dest\app\(dashboard)\ventes\devis\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/ventes/devis/page.tsx copié"

# Caisses
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\ventes\caisses" | Out-Null
Copy-Item "$source\app\(dashboard)\ventes\caisses\page.tsx" "$dest\app\(dashboard)\ventes\caisses\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/ventes/caisses/page.tsx copié"

# Avoirs
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\factures\avoirs" | Out-Null
Copy-Item "$source\app\(dashboard)\factures\avoirs\page.tsx" "$dest\app\(dashboard)\factures\avoirs\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/factures/avoirs/page.tsx copié"

# Inventaires
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\stocks\inventaires" | Out-Null
Copy-Item "$source\app\(dashboard)\stocks\inventaires\page.tsx" "$dest\app\(dashboard)\stocks\inventaires\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/stocks/inventaires/page.tsx copié"

# Trésorerie
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\tresorerie\releve-client" | Out-Null
Copy-Item "$source\app\(dashboard)\tresorerie\releve-client\page.tsx" "$dest\app\(dashboard)\tresorerie\releve-client\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/tresorerie/releve-client/page.tsx copié"

New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\tresorerie\balance-agee" | Out-Null
Copy-Item "$source\app\(dashboard)\tresorerie\balance-agee\page.tsx" "$dest\app\(dashboard)\tresorerie\balance-agee\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/tresorerie/balance-agee/page.tsx copié"

New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\tresorerie\echeancier" | Out-Null
Copy-Item "$source\app\(dashboard)\tresorerie\echeancier\page.tsx" "$dest\app\(dashboard)\tresorerie\echeancier\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/tresorerie/echeancier/page.tsx copié"

New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\tresorerie\remises" | Out-Null
Copy-Item "$source\app\(dashboard)\tresorerie\remises\page.tsx" "$dest\app\(dashboard)\tresorerie\remises\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/tresorerie/remises/page.tsx copié"

# Catégories
New-Item -ItemType Directory -Force -Path "$dest\app\(dashboard)\stocks\categories" | Out-Null
Copy-Item "$source\app\(dashboard)\stocks\categories\page.tsx" "$dest\app\(dashboard)\stocks\categories\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/stocks/categories/page.tsx copié"

# Nouveau Produit
Copy-Item "$source\app\(dashboard)\produits\nouveau\page.tsx" "$dest\app\(dashboard)\produits\nouveau\page.tsx" -Force
Write-Host "[OK] app/(dashboard)/produits/nouveau/page.tsx copié"

# Navigation config
Copy-Item "$source\config\navigation.ts" "$dest\config\navigation.ts" -Force
Write-Host "[OK] config/navigation.ts mis à jour"

Write-Host "Synchronisation terminee avec succes !"
