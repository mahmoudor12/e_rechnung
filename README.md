# 📄 E-Rechnung Validator Service

Ein **Spring Boot Microservice** zur Validierung, Verarbeitung und Archivierung von elektronischen Rechnungen gemäß der deutschen **E-Rechnungspflicht (B2B)**. Das System unterstützt **XRechnung (UBL & CII)** und **ZUGFeRD** und nutzt den offiziellen **KoSIT-Validator** für die Konformitätsprüfung.

---

## 🚀 Features

- ✅ **KoSIT-Validator** – offizielle Validierung für XRechnung 3.0.2
- ✅ **Unterstützung für XRechnung (UBL & CII)** und ZUGFeRD
- ✅ **Asynchrone Verarbeitung** eingehender Rechnungen
- ✅ **Revisionssichere Archivierung** (GoBD-konform)
- ✅ **REST-API** für Inbound (Peppol-Webhooks)
- ✅ **Datenbank-Persistenz** mit JPA & PostgreSQL
- ✅ **Next.js-Frontend** (Dashboard für Upload, Status und Fehleranzeige)
- ✅ **CORS-konform** für lokale Entwicklung
- ✅ **Docker-Compose** für einfache Datenbank-Infrastruktur

---

## 🛠️ Technologie-Stack

| Technologie | Version |
|-------------|---------|
| **Java** | 21+ (getestet mit 23) |
| **Spring Boot** | 4.1.0 |
| **Hibernate** | 7.4.1 |
| **PostgreSQL** | 17.4 |
| **KoSIT-Validator** | 1.5.1 |
| **Mustangproject** | 2.20.0 |
| **Next.js** | 14+ |
| **Tailwind CSS** | 3.x |
| **Docker Compose** | – |

---

## 📁 Projektstruktur (Auszug)
