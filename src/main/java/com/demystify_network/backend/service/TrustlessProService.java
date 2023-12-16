package com.demystify_network.backend.service;

import com.demystify_network.backend.api.request.AddressRequest;
import com.demystify_network.backend.api.response.ProScoreKnowledgeGraphResponse;
import com.demystify_network.backend.model.Insights;

public interface TrustlessProService {

  ProScoreKnowledgeGraphResponse proScore(AddressRequest request, Insights insights);
}
