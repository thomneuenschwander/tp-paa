#!/bin/bash

OUTPUT_FILE="execution_times.log.csv"
JAVA_PROGRAM_CLASS="tests.RunTests" 

GRAPH_DIR="./tests" 

GRAPHS=("graph_test_n5.txt" "graph_test_n15.txt" "graph_test_n25.txt")


TECHNIQUES=("-bf" "-heur" "-bnb")

ROOT_VERTEX_MAX_CYCLE="1"
PROBLEMS_TO_RUN=("1" "2")

if ! command -v java &> /dev/null; then
    echo "Erro: Comando java não encontrado."
    exit 1
fi

# --- Gerenciamento de Arquivos Temporários ---
TEMP_FILES=()
cleanup() {
    for tmp_file in "${TEMP_FILES[@]}"; do
        rm -f "$tmp_file"
    done
}
trap cleanup EXIT SIGINT SIGTERM

mktemp_register() {
    local tmp_file
    tmp_file=$(mktemp)
    if [ -z "$tmp_file" ] || [ ! -f "$tmp_file" ]; then
        echo "Erro: mktemp falhou ao criar um arquivo temporário."
        exit 1
    fi
    TEMP_FILES+=("$tmp_file")
    echo "$tmp_file"
}

echo "Iniciando benchmark do RunTests..."
echo "Os resultados serão salvos em: $OUTPUT_FILE"
echo "Grafos: ${GRAPHS[*]}"
echo "Técnicas: ${TECHNIQUES[*]}"
echo "Problemas: ${PROBLEMS_TO_RUN[*]}"
echo ""

# Cabeçalho do CSV
echo "ProblemType,GraphFile,Technique,RootVertex,ExecutionTime_s,RunTestsSummary" > "$OUTPUT_FILE"

for problem_p_flag in "${PROBLEMS_TO_RUN[@]}"; do
    problem_name=$([ "$problem_p_flag" == "1" ] && echo "MaxCycle" || echo "MDS")

    echo "Testando: $problem_name (-p $problem_p_flag)" 
    echo "==========================================="

    for graph_file_basename in "${GRAPHS[@]}"; do
        graph_full_path="$GRAPH_DIR/$graph_file_basename"

        if [ ! -f "$graph_full_path" ]; then
            echo "AVISO: Arquivo de grafo $graph_full_path não encontrado. Pulando."
            echo "$problem_name,$graph_file_basename,N/A,N/A,N/A,\"Arquivo de grafo nao encontrado\"" >> "$OUTPUT_FILE"
            continue
        fi

        for technique in "${TECHNIQUES[@]}"; do
            current_root_vertex="N/A" 
            if [ "$problem_p_flag" == "1" ]; then
                current_root_vertex="$ROOT_VERTEX_MAX_CYCLE"
            fi

            echo "🔄  Processando: Problema=$problem_name, Grafo='$graph_file_basename', Técnica='$technique', Raiz(se p = 1)='$current_root_vertex'"

            sync 
            if sudo -n sh -c "sync && echo 3 > /proc/sys/vm/drop_caches" 2>/dev/null; then
              echo "   Caches de disco limpos (via sudo -n)."
            else
              echo "   Não foi possível limpar caches de disco (sem sudo -n ou falhou)."
            fi

            # --- Montar e Executar Comando Java ---
            COMMAND_OUTPUT_TEMP=$(mktemp_register)
            TIME_OUTPUT_TEMP=$(mktemp_register)

            JAVA_CMD_ARGS="-p $problem_p_flag \"$graph_full_path\""
            if [ "$problem_p_flag" == "1" ]; then
                JAVA_CMD_ARGS="$JAVA_CMD_ARGS \"$current_root_vertex\""
            fi
            JAVA_CMD_ARGS="$JAVA_CMD_ARGS \"$technique\""

            JAVA_CMD_LINE="java $JAVA_PROGRAM_CLASS $JAVA_CMD_ARGS"

            echo "   Executando: $JAVA_CMD_LINE"

            /usr/bin/time -f "%e" -o "$TIME_OUTPUT_TEMP" \
                sh -c "$JAVA_CMD_LINE" > "$COMMAND_OUTPUT_TEMP" 2>&1

            execution_time=$(cat "$TIME_OUTPUT_TEMP")
            runtests_full_output=$(cat "$COMMAND_OUTPUT_TEMP")

            # --- Extrair Resumo da Saída do RunTests ---
            key_info=""
            if echo "$runtests_full_output" | grep -q -E "Erro:|Invalid flag:|Exception|NullPointerException|ArrayIndexOutOfBoundsException|Nao foi possivel carregar o grafo"; then
                key_info=$(echo "$runtests_full_output" | grep -m 1 -E "Erro:|Invalid flag:|Exception|NullPointerException|ArrayIndexOutOfBoundsException|Nao foi possivel carregar o grafo")

            # Saídas específicas do problema
            elif [ "$problem_p_flag" == "1" ]; then # Max Cycle
                if echo "$runtests_full_output" | grep -q "Nenhum ciclo encontrado"; then
                    key_info=$(echo "$runtests_full_output" | grep -m 1 "Nenhum ciclo encontrado")
                elif echo "$runtests_full_output" | grep -q "Tamanho Máximo do Ciclo:"; then
                    size_line=$(echo "$runtests_full_output" | grep "Tamanho Máximo do Ciclo:")
                    path_line=$(echo "$runtests_full_output" | grep "Caminho do Ciclo:")
                    key_info="$size_line${path_line:+; $path_line}" 
                fi
            elif [ "$problem_p_flag" == "2" ]; then # MDS
                if echo "$runtests_full_output" | grep -q "Nenhum conjunto dominante encontrado"; then
                    key_info=$(echo "$runtests_full_output" | grep -m 1 "Nenhum conjunto dominante encontrado")
                elif echo "$runtests_full_output" | grep -q "Tamanho do Conjunto Dominante:"; then
                    size_line=$(echo "$runtests_full_output" | grep "Tamanho do Conjunto Dominante:")
                    set_line=$(echo "$runtests_full_output" | grep "Conjunto Dominante:")
                    # Verifica se há uma linha de resultado interno do BnB e a prioriza se for melhor
                    bnb_internal_size_line=$(echo "$runtests_full_output" | grep "mdsFinder.minDominatingSet.*Tamanho:")
                    if [ -n "$bnb_internal_size_line" ]; then
                        bnb_internal_set_line=$(echo "$runtests_full_output" | grep "mdsFinder.minDominatingSet.*Conjunto:")
                        key_info="$bnb_internal_size_line${bnb_internal_set_line:+; $bnb_internal_set_line}"
                    else
                        key_info="$size_line${set_line:+; $set_line}"
                    fi
                fi
            fi

            if [ -z "$key_info" ]; then
                key_info=$(echo "$runtests_full_output" | head -n 3 | tr '\n' ' ') # Fallback
                if [ -z "$key_info" ]; then
                    key_info="RunTests_Output_Indisponivel_Ou_Vazio"
                fi
            fi

            summary_csv_field=$(echo "$key_info" | tr '\n' ' ' | sed 's/"/""/g' | sed 's/,/;/g' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

            # --- Adicionar Resultado ao Arquivo de Saída ---
            echo "$problem_name,$graph_file_basename,$technique,$current_root_vertex,$execution_time,\"$summary_csv_field\"" >> "$OUTPUT_FILE"

            echo "   ⏱️  Tempo de Execução: $execution_time s"
            echo "   📊 Resumo (para CSV): \"$summary_csv_field\""
            echo "   --- Saída Completa do RunTests ---"
            echo "$runtests_full_output"
            echo "   --- Fim da Saída Completa ---"
            echo "   ----------------------------------"

            sleep 1 
        done #
        echo "" 
    done 
    echo ""
done 

echo "✅ Benchmark concluído."
echo "Resultados salvos em $OUTPUT_FILE"

exit 0