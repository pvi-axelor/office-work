package com.axelor.invoice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axelor.invoice.db.Invoice;

public interface InvoiceService {

  public void cancelSelectedInvoices(ArrayList<Integer> list);

  public Optional<Invoice> mergeInvoices(List<Integer> list);
}
