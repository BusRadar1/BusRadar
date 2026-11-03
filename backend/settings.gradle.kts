pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "busradar-backend"

include("libs:events")
include("services:ingest-service")
include("services:tracking-service")
include("services:stats-service")
include("services:trips-service")