package com.axelor.invoice.web;

import com.axelor.invoice.db.Invoice;
import com.axelor.invoice.db.InvoiceLine;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;

public class InvoiceLineController {

  public void setTaxes(ActionRequest request, ActionResponse response) {
    try {
      Invoice invoice = request.getContext().getParent().asType(Invoice.class);
      if (invoice.getCustomer().getIsSubjectToTaxes() == false)
        response.setAttr("taxRate", "hidden", true);
      response.setValue("taxRate", 0.2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setDescriptionAndUntaxedPrice(ActionRequest request, ActionResponse response) {

    try {
      InvoiceLine invoiceLine = request.getContext().asType(InvoiceLine.class);

      response.setValue("description", invoiceLine.getProduct().getName());
      response.setValue("unitPriceUntaxed", invoiceLine.getProduct().getUnitPriceUntaxed());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void calculateTotal(ActionRequest request, ActionResponse response) {

    try {
      InvoiceLine invoiceLine = request.getContext().asType(InvoiceLine.class);
      BigDecimal totalWithoutTax =
          invoiceLine.getQuantity().multiply(invoiceLine.getUnitPriceUntaxed());

      BigDecimal totalWithTax =
          totalWithoutTax.add(totalWithoutTax.multiply(invoiceLine.getTaxRate()));

      response.setValue("inTaxTotal", totalWithTax);
      response.setValue("exTaxTotal", totalWithoutTax);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
