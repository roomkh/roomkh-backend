package com.roomkh.backend.validation;

import com.roomkh.backend.entity.PriceUnit;
import com.roomkh.backend.entity.PropertyPurpose;

public interface PropertyPriceFields {
    PropertyPurpose getPurpose();
    PriceUnit getPriceUnit();
}