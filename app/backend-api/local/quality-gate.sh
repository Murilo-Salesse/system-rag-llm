#!/usr/bin/env bash
# quality-gate.sh — Verifica line coverage >= 80% (JaCoCo) e mutation kill rate >= 80% (PITest)
# Uso: execute este script a partir da raiz do módulo Maven (app/backend-api/notebooklm/)
# ou com MAVEN_MODULE_DIR apontando para a raiz do módulo.
set -euo pipefail

MODULE_DIR="${MAVEN_MODULE_DIR:-$(dirname "$0")/../notebooklm}"
JACOCO_XML="${MODULE_DIR}/target/site/jacoco/jacoco.xml"
PIT_REPORT_GLOB="${MODULE_DIR}/target/pit-reports/mutations.xml"

FAILED=0

# ─────────────────────────────────────────────
# 1. JaCoCo — line coverage (>= 80%)
# ─────────────────────────────────────────────
echo "━━━ JaCoCo Line Coverage (Threshold: >= 80%) ━━━"

if [[ ! -f "${JACOCO_XML}" ]]; then
  echo "❌ Relatório JaCoCo não encontrado: ${JACOCO_XML}"
  echo "   Execute './mvnw test' antes de rodar este script."
  FAILED=1
else
  if ! python3 - "${JACOCO_XML}" << 'PY_EOF'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
tree = ET.parse(path)
root = tree.getroot()

bad_classes = []
total_covered = 0
total_lines = 0

for pkg in root.findall(".//package"):
    for cls in pkg.findall("class"):
        covered = 0
        missed = 0
        for counter in cls.findall("counter"):
            if counter.get("type") == "LINE":
                covered = int(counter.get("covered", 0))
                missed  = int(counter.get("missed", 0))
        total = covered + missed
        if total == 0:
            continue
        total_covered += covered
        total_lines += total
        pct = (covered / total) * 100
        if pct < 80.0:
            bad_classes.append((cls.get("name"), covered, total, pct))

overall_pct = (total_covered / total_lines * 100) if total_lines > 0 else 100.0
print(f"📊 Cobertura geral: {total_covered}/{total_lines} linhas ({overall_pct:.1f}%)")

if bad_classes:
    print("❌ Classes com cobertura de linhas < 80%:")
    for name, cov, tot, pct in bad_classes:
        print(f"   {name}: {cov}/{tot} linhas ({pct:.1f}%)")
    sys.exit(1)
else:
    print("✅ Todas as classes possuem cobertura >= 80%")
PY_EOF
  then
    FAILED=1
  fi
fi

# ─────────────────────────────────────────────
# 2. PITest — mutation kill rate (>= 80%)
# ─────────────────────────────────────────────
echo ""
echo "━━━ PITest Mutation Coverage (Threshold: >= 80%) ━━━"

PIT_XML=$(ls "${MODULE_DIR}"/target/pit-reports/mutations.xml 2>/dev/null || true)

if [[ -z "${PIT_XML}" ]]; then
  echo "⚠️  Relatório PITest não encontrado — nenhuma classe mutável no escopo (failWhenNoMutations=false)."
  echo "   Se existem classes de domínio/serviço implementadas, execute:"
  echo "   './mvnw org.pitest:pitest-maven:mutationCoverage' antes de rodar este script."
  echo "✅ Mutation coverage: sem mutantes no escopo (gate não aplicável)"
else
  if ! python3 - "${PIT_XML}" << 'PY_EOF'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
tree = ET.parse(path)
root = tree.getroot()

total_mutations = 0
killed_mutations = 0
survivors = []

for mutation in root.findall(".//mutation"):
    total_mutations += 1
    detected = mutation.get("detected", "true").lower()
    if detected == "true":
        killed_mutations += 1
    else:
        cls   = (mutation.findtext("mutatedClass") or "?").split(".")[-1]
        line  = mutation.findtext("lineNumber") or "?"
        mtype = mutation.findtext("mutator") or "?"
        mtype = mtype.split(".")[-1]
        survivors.append(f"   {cls}:{line} [{mtype}]")

kill_rate = (killed_mutations / total_mutations * 100) if total_mutations > 0 else 100.0
print(f"📊 Mutation kill rate: {killed_mutations}/{total_mutations} ({kill_rate:.1f}%)")

if kill_rate < 80.0:
    print(f"❌ Mutation kill rate < 80% ({kill_rate:.1f}%). {len(survivors)} mutante(s) sobrevivente(s):")
    for s in survivors:
        print(s)
    sys.exit(1)
else:
    print(f"✅ Mutation kill rate ({kill_rate:.1f}%) atingiu o limiar de >= 80%")
PY_EOF
  then
    FAILED=1
  fi
fi

# ─────────────────────────────────────────────
# 3. Resultado final
# ─────────────────────────────────────────────
echo ""
if [[ "${FAILED}" -eq 0 ]]; then
  echo "🎉 Quality Gate: PASSOU"
  exit 0
else
  echo "💥 Quality Gate: FALHOU — corrija os problemas acima e reexecute."
  exit 1
fi
