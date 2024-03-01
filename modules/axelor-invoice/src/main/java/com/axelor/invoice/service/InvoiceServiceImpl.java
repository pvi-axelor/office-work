package com.axelor.invoice.service;

import com.axelor.inject.Beans;
import com.axelor.invoice.db.Invoice;
import com.axelor.invoice.db.InvoiceLine;
import com.axelor.invoice.db.repo.InvoiceLineRepository;
import com.axelor.invoice.db.repo.InvoiceRepository;
import com.axelor.sales.db.Products;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvoiceServiceImpl implements InvoiceService {

  @Override
  @Transactional(rollbackOn = Exception.class)
  public void cancelSelectedInvoices(ArrayList<Integer> list) {

    try {

      for (long i : list) {
        Invoice temp = Beans.get(InvoiceRepository.class).find((long) i);
        if (temp.getStatusSelect() < 2) temp.setStatusSelect(3);
        Beans.get(InvoiceRepository.class).merge(temp);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  @Transactional(rollbackOn = Exception.class)
  public Optional<Invoice> mergeInvoices(List<Integer> list) {

    try {
      ArrayList<Invoice> invoiceList = new ArrayList<>();

      for (long i : list) {
        invoiceList.add(Beans.get(InvoiceRepository.class).find((long) i));
      }

      if (invoiceList.stream().anyMatch(t -> t.getCustomer() != invoiceList.get(0).getCustomer())) {
    	  return Optional.empty();
      }

      Map<String, Object> newInvoiceInstanceMap =
          Map.of(
              "customer", invoiceList.get(0).getCustomer(),
              "invoiceDateT", LocalDateTime.now(),
              "statusSelect", 0);

      Invoice newInvoice = Beans.get(InvoiceRepository.class).create(newInvoiceInstanceMap);

      invoiceList.stream()
          .forEach(
              it -> {
                it.setArchived(true);
                it.setGeneratedInvoice(newInvoice);
              });

      List<InvoiceLine> invoiceLineList =
          invoiceList.stream()
              .map(Invoice::getInvoiceLineList)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

      Map<Products, List<InvoiceLine>> productMap =
          invoiceLineList.stream().collect(Collectors.groupingBy(InvoiceLine::getProduct));

      Map<Products, List<InvoiceLine>> mergedMap = new HashMap<Products, List<InvoiceLine>>();

      for (Products i : productMap.keySet().stream().collect(Collectors.toList())) {

        List<InvoiceLine> currentInvoiceLineList = productMap.get(i);
        List<InvoiceLine> mergedList = new ArrayList<InvoiceLine>();

        for (int j = 0; j < currentInvoiceLineList.size(); ++j) {
          InvoiceLine currentFromMainList = currentInvoiceLineList.get(j);          
          InvoiceLine mergedInvoiceLine = Beans.get(InvoiceLineRepository.class).create(null);
          
          Boolean matchFound = false;

          for (int k = 0; k < mergedList.size(); ++k) {
            InvoiceLine currentFromMergedList = mergedList.get(k);
            matchFound = false;

            if (currentFromMainList.getTaxRate().compareTo(currentFromMergedList.getTaxRate()) == 0
                && currentFromMainList
                    .getUnitPriceUntaxed()
                    .equals(currentFromMergedList.getUnitPriceUntaxed())
                && currentFromMainList
                    .getDescription()
                    .equals(currentFromMergedList.getDescription())) {

              currentFromMergedList.setQuantity(
                  currentFromMergedList.getQuantity().add(currentFromMainList.getQuantity()));

              currentFromMergedList.setExTaxTotal(
                  currentFromMergedList.getExTaxTotal().add(currentFromMainList.getExTaxTotal()));

              currentFromMergedList.setInTaxTotal(
                  currentFromMergedList.getInTaxTotal().add(currentFromMainList.getInTaxTotal()));

              matchFound = true;
              break;
            }
          }

          if (!matchFound) {
        	  
        	mergedInvoiceLine.setDescription(currentFromMainList.getDescription());
        	mergedInvoiceLine.setExTaxTotal(currentFromMainList.getExTaxTotal());
        	mergedInvoiceLine.setInTaxTotal(currentFromMainList.getInTaxTotal());
        	mergedInvoiceLine.setQuantity(currentFromMainList.getQuantity());
        	mergedInvoiceLine.setTaxRate(currentFromMainList.getTaxRate());
        	mergedInvoiceLine.setUnitPriceUntaxed(currentFromMainList.getUnitPriceUntaxed());
        	mergedInvoiceLine.setProduct(currentFromMainList.getProduct());
        	mergedInvoiceLine.setInvoice(newInvoice);       	  
            mergedList.add(mergedInvoiceLine);
          }
          
        }

        mergedList.stream().filter(it -> it.getId() == null).forEach(it -> Beans.get(InvoiceLineRepository.class).save(it));
        mergedMap.put(i, mergedList);
      }

      List<InvoiceLine> mergedInvoiceLineList =
          mergedMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
      
      newInvoice.setMergedInvoiceList(invoiceList);
      newInvoice.setInvoiceLineList(mergedInvoiceLineList);
      newInvoice.setExTaxTotal(
          mergedInvoiceLineList.stream()
              .map(InvoiceLine::getExTaxTotal)
              .reduce(BigDecimal.ZERO, BigDecimal::add));
      newInvoice.setInTaxTotal(
          mergedInvoiceLineList.stream()
              .map(InvoiceLine::getInTaxTotal)
              .reduce(BigDecimal.ZERO, BigDecimal::add));

      Beans.get(InvoiceRepository.class).save(newInvoice);

      return Optional.of(newInvoice);
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
