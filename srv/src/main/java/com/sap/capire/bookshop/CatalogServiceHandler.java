package com.sap.capire.bookshop;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Custom logic for the CatalogService 'submitOrder' action.
 *
 * This handler uses the generic (untyped) CQN API so that it compiles
 * WITHOUT relying on classes produced by the cds-maven-plugin 'generate'
 * goal. It decrements the stock of a book if enough is available.
 *
 * Entity/element names ("sap.capire.bookshop.Books", "ID", "stock") come
 * straight from db/schema.cds, so no generated code is required.
 */
@Component
@ServiceName("CatalogService")
public class CatalogServiceHandler implements EventHandler {

    private static final String BOOKS = "sap.capire.bookshop.Books";

    @On(event = "submitOrder")
    public void onSubmitOrder(com.sap.cds.services.handler.EventContext context) {
        CqnService service = (CqnService) context.getService();

        Map<String, Object> params = context.get("params") instanceof Map
            ? castParams(context.get("params"))
            : Map.of();

        Object bookParam = context.get("book");
        Object qtyParam = context.get("quantity");
        Integer bookId = toInt(bookParam);
        Integer quantity = toInt(qtyParam);

        if (bookId == null || quantity == null) {
            context.getMessages().error("Both 'book' and 'quantity' are required");
            return;
        }

        Result book = service.run(
            Select.from(BOOKS)
                  .columns("stock")
                  .where(b -> b.get("ID").eq(bookId)));

        if (book.rowCount() == 0) {
            context.getMessages().error("Book #" + bookId + " doesn't exist");
            return;
        }

        Optional<Object> stockVal = book.first().map(r -> r.get("stock"));
        int stock = stockVal.map(v -> ((Number) v).intValue()).orElse(0);

        if (quantity > stock) {
            context.getMessages().error(quantity + " exceeds stock for book #" + bookId);
            return;
        }

        int newStock = stock - quantity;
        service.run(Update.entity(BOOKS)
                          .data(Map.of("stock", newStock))
                          .where(b -> b.get("ID").eq(bookId)));

        context.put("result", Map.of("stock", newStock));
        context.setCompleted();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castParams(Object o) {
        return (Map<String, Object>) o;
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
