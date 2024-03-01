package com.axelor.invoice.web;

import com.axelor.inject.Beans;
import com.axelor.invoice.db.Invoice;
import com.axelor.invoice.service.InvoiceService;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvoiceController {

  public void setInvoiceDateTime(ActionRequest request, ActionResponse response) {
    try {
      response.setValue("invoiceDateT", LocalDateTime.now());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void priceWithTaxTotal(ActionRequest request, ActionResponse response) {

    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      BigDecimal sumInTaxTotal = BigDecimal.ZERO;

      for (var i : invoice.getInvoiceLineList())
        sumInTaxTotal = i.getInTaxTotal().add(sumInTaxTotal);

      response.setValue("inTaxTotal", sumInTaxTotal);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void priceWithoutTaxTotal(ActionRequest request, ActionResponse response) {

    try {
      Invoice invoice = request.getContext().asType(Invoice.class);
      BigDecimal sumExTaxTotal = BigDecimal.ZERO;

      for (var i : invoice.getInvoiceLineList())
        sumExTaxTotal = i.getExTaxTotal().add(sumExTaxTotal);

      response.setValue("exTaxTotal", sumExTaxTotal);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void validateInvoice(ActionRequest request, ActionResponse response) {

    try {
      Invoice invoice = request.getContext().asType(Invoice.class);

      var invoiceList = invoice.getInvoiceLineList();

      if (invoiceList.isEmpty()) {
        response.setError("At least one invoice line is required");
        return;
      }

      if (invoiceList.stream().anyMatch(it -> it.getExTaxTotal().compareTo(BigDecimal.ONE) < 0)) {
        response.setError("One invoice line has a null or negative total");
        return;
      }

      this.priceWithoutTaxTotal(request, response);
      this.priceWithTaxTotal(request, response);

      response.setValue("statusSelect", 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void ventilateInvoice(ActionRequest request, ActionResponse response) {

    try {
      this.validateInvoice(request, response);
      response.setValue("statusSelect", 2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void cancelInvoice(ActionRequest request, ActionResponse response) {

    try {
      this.priceWithoutTaxTotal(request, response);
      this.priceWithTaxTotal(request, response);

      response.setAlert("This action will cancel this invoice. Do you want to proceed?");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setStatusCancel(ActionRequest request, ActionResponse response) {

    try {
      response.setAttr("statusSelect", "value", 3);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void cancelSelectedInvoices(ActionRequest request, ActionResponse response) {
    try {

      @SuppressWarnings("unchecked")
      ArrayList<Integer> list = (ArrayList<Integer>) request.getContext().get("_ids");

      if (list == null || list.isEmpty()) return;
      Beans.get(InvoiceService.class).cancelSelectedInvoices(list);

      response.setReload(true);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void mergeInvoices(ActionRequest request, ActionResponse response) {
    try {

      @SuppressWarnings("unchecked")
      ArrayList<LinkedHashMap<String, Object>> hashMapList =
          (ArrayList<LinkedHashMap<String, Object>>) request.getContext().get("mergeField");

      List<Integer> list =
          hashMapList.stream()
              .flatMap(hmap -> hmap.values().stream())
              .filter(it -> it instanceof Integer)
              .map(obj -> (Integer) obj)
              .collect(Collectors.toList());

      if (list == null || list.isEmpty()) {
        return;
      }
      
      Optional<Invoice> mergedInvoice = Beans.get(InvoiceService.class).mergeInvoices(list);

      if (mergedInvoice.isPresent()) {
    	  response.setView(ActionView.define("Invoice").model("com.axelor.invoice.db.Invoice").add("form", "invoice-form").context("_showRecord", mergedInvoice.get().getId()).map());
    	  response.setCanClose(true);    	  
      } else {
    	  response.setError("You have to choose same customer.");
      }
      

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
