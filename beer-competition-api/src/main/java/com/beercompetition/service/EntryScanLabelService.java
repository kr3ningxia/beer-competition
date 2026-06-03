package com.beercompetition.service;

import com.beercompetition.pojo.po.BeerEntry;
import com.beercompetition.pojo.po.EntryScanLabel;

import java.util.Collection;
import java.util.Map;

public interface EntryScanLabelService {

    EntryScanLabel createActiveLabel(BeerEntry entry, Long generatedBy);

    EntryScanLabel requireActiveLabel(Long beerEntryId);

    EntryScanLabel resolveActiveLabel(String code);

    Map<Long, EntryScanLabel> listActiveLabels(Collection<Long> beerEntryIds);
}
