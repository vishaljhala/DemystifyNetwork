package com.demystify.network.backend.service;

import com.demystify.network.backend.api.request.AddressRequest;
import com.demystify.network.backend.api.response.ProScoreKnowledgeGraphResponse;
import com.demystify.network.backend.model.Insights;

public interface TrustlessProService {

  ProScoreKnowledgeGraphResponse proScore(AddressRequest request, Insights insights);
}
