# ERP/CRM Project — Claude Code Context

## Project Overview
This is a **Real Estate ERP/CRM System** for managing inventory, indents,
purchase orders, inward/outward movements, and stock operations.

---

## Repo Structure (Single Repository)

**Root Path:**
`/Users/bsridharpatnaik/GitHub/RealEstate-ERP-CRM-System`

### Backend — Spring Boot (Microservices)

#### Inventory Service
- **Path:** `sc-inventory-service/src`
- **Tech:** Java, Spring Boot, JPA/Hibernate, MySQL (multi-tenant)
- **Modules:** Inventory, Indents, Purchase Orders, Inward, Outward, Stock

#### CRM Service
- **Path:** `sc-crm-service/src`
- **Tech:** Java, Spring Boot

#### Common Service
- **Path:** `sc-common-service/src`
- **Purpose:** Shared utilities, configs, and common logic

---

### Frontend — React
- **Path:** `SC UI/src`
- **Tech:** React (JavaScript/TypeScript)
- **Modules:** Inventory UI, Indent management, PO screens, Stock dashboard

---

## Scope Instructions

> **Only consider the following paths:**
- `sc-inventory-service/src`
- `SC UI/src`

> Do NOT read or suggest changes in:
- `sc-crm-service`
- `sc-common-service`
- any other directory under the repo  
unless explicitly asked.

---

## How to Respond to Requirements & Bugs

When I describe a **requirement or bug**, always:

1. **Understand the feature/issue** — ask clarifying questions if needed  

2. **Backend changes first**  
   - Focus on `sc-inventory-service/src`  
   - Identify controller, service, repository, and entity files  
   - Show exact file paths relative to `sc-inventory-service/src`  

3. **Frontend changes next**  
   - Focus on `SC UI/src`  
   - Identify components, services, and API integrations  
   - Show exact file paths relative to `SC UI/src`  

4. **Keep changes in sync**  
   - If backend API contract changes (request/response, endpoints),  
     update frontend API calls accordingly  

5. **Call out side effects**  
   - DB schema changes  
   - Config updates  
   - New dependencies  

---

## Architecture Notes

- Backend is **multi-tenant** — schemas are prefixed per tenant  
- Custom ID generation is used — do not assume auto-increment IDs  
- PDF generation uses **iText** (purchase orders)  
- Frontend communicates with backend via REST APIs  
- Backend deployed on **Tomcat/VPS** — avoid changes that break WAR packaging  

---

## Response Format Preference

- Show file paths clearly before each code block  
- Prefer minimal diffs over full file rewrites  
- If a change spans multiple files, list all affected files upfront  
- Do not directly modify code. Show me the changes first. After approval, apply the changes.  

## Important Constraints

- Do NOT introduce new frameworks or libraries unless asked
- Do NOT refactor unrelated code
- Do NOT change existing API contracts unless explicitly required
- Do NOT assume DB schema changes unless specified
- Prefer modifying existing files over creating new ones

## Graphify

This project has a Graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md first.
- If graphify-out/wiki/index.md exists, prefer it over raw file scanning.
- After code changes, rebuild graph:

python3 -c "from graphify.watch import _rebuild_code; from pathlib import Path; _rebuild_code(Path('.'))"

## Response Style

Default for all repo tasks:
- concise
- direct
- implementation-first
- minimal filler
- short explanations unless requested
- preserve technical accuracy
- keep code unchanged unless editing requested
- for feature requests: give exact files + changes first
- for bugs: root cause first, then fix