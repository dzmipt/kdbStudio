package studio.kdb.query;

import studio.kdb.K;
import studio.ui.StudioWindow;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Schema {

    private final Map<String, List<String>> schema;
    private final String[] autoCompletionList;
    private final Instant created;

    public Schema(QueryResult queryResult) throws SchemaParseException {
        created = Instant.now();

        this.schema = new HashMap<>();

        K.KBase result = queryResult.getResult();
        if (result == null) throw new SchemaParseException("Result is null");

        try {
            K.Dict dict = (K.Dict)result;
            K.KSymbolVector tables = (K.KSymbolVector) dict.x;
            K.KList values = (K.KList) dict.y;

            int size = tables.count();
            for (int i=0; i<size; i++) {
                K.KSymbolVector cols = (K.KSymbolVector)values.at(i);
                String[] colNames = (String[]) cols.getArray();
                schema.put(tables.at(i).s, List.of(colNames));
            }
        } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
            throw new SchemaParseException("Unexpected format of the result", e);
        }

        autoCompletionList = Stream.concat(
                                schema.values().stream().flatMap(List::stream),
                                schema.keySet().stream()
                            ).distinct()
                                .sorted()
                                .toArray(String[]::new);
    }

    public static QueryTask getQueryTask(StudioWindow studioWindow) {
        return new LoadDataSchema(studioWindow);
    }

    public String[] getAutoCompletionList() {
        return autoCompletionList;
    }

    public Instant getCreated() {
        return created;
    }

    private static class LoadDataSchema extends QueryTask.Query {

        private final static String query = "tables[]!cols each tables[]";

        LoadDataSchema(StudioWindow studioWindow) {
            super(studioWindow, query, false);
        }

        @Override
        public String getQueryText() {
            return "<load data schema>";
        }
    }

}
