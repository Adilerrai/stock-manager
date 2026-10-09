group "default" {
  targets = ["omega", "saas"]
}

target "base" {
  context    = "."
  dockerfile = "Dockerfile"
}

# Image pour Omega Brand
target "omega" {
  inherits = ["base"]
  tags     = ["omega-app-api:latest"]
}

# Image pour SaaS Global
target "saas" {
  inherits = ["base"]
  tags     = ["saas-app-api:latest"]
}
