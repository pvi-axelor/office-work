package com.axelor.invoice.module;

import com.axelor.app.AxelorModule;
import com.axelor.invoice.service.InvoiceService;
import com.axelor.invoice.service.InvoiceServiceImpl;

public class InvoiceModule extends AxelorModule {
  @Override
  public void configure() {
    bind(InvoiceService.class).to(InvoiceServiceImpl.class);
  }
}
