docker exec -i $1 psql -v ON_ERROR_STOP=1 -U rutherford -f - --quiet
