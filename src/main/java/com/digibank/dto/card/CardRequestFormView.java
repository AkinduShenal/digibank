package com.digibank.dto.card;

import com.digibank.enums.CardType;

import java.util.List;

public record CardRequestFormView(List<CardAccountOption> accounts, CardType[] cardTypes) {
}
