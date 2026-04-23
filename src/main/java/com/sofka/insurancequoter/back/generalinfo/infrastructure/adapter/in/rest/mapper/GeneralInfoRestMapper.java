package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.generalinfo.application.usecase.UpdateGeneralInfoCommand;
import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.GeneralInfoResponse;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.InsuredDataDto;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.UnderwritingDataDto;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.UpdateGeneralInfoRequest;
import org.springframework.stereotype.Component;

// Maps between domain objects and REST DTOs for the general-info bounded context
@Component
public class GeneralInfoRestMapper {

    public GeneralInfoResponse toResponse(GeneralInfo domain) {
        return new GeneralInfoResponse(
                domain.folioNumber(),
                domain.quoteStatus(),
                new InsuredDataDto(
                        domain.insuredData().name(),
                        domain.insuredData().rfc(),
                        domain.insuredData().email(),
                        domain.insuredData().phone()
                ),
                new UnderwritingDataDto(
                        domain.underwritingInfo().subscriberId(),
                        domain.underwritingInfo().agentCode(),
                        domain.underwritingInfo().riskClassification() != null
                                ? domain.underwritingInfo().riskClassification().name() : null,
                        domain.underwritingInfo().businessType() != null
                                ? domain.underwritingInfo().businessType().name() : null
                ),
                domain.updatedAt(),
                domain.version()
        );
    }

    public UpdateGeneralInfoCommand toCommand(String folioNumber, UpdateGeneralInfoRequest request) {
        return new UpdateGeneralInfoCommand(
                folioNumber,
                new InsuredData(
                        request.insuredData().name(),
                        request.insuredData().rfc(),
                        request.insuredData().email(),
                        request.insuredData().phone()
                ),
                new UnderwritingInfo(
                        request.underwritingData().subscriberId(),
                        request.underwritingData().agentCode(),
                        RiskClassification.valueOf(request.underwritingData().riskClassification()),
                        BusinessType.valueOf(request.underwritingData().businessType())
                ),
                request.version()
        );
    }
}
