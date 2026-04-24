package com.sofka.insurancequoter.back.coverage.domain.service;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pure domain service — no Spring annotations, no infrastructure dependencies.
// Derives CoverageOption list from active guarantee codes and catastrophic zone flag.
//
// Mapping table (from domain spec):
//   GUA-FIRE or GUA-CONT (active) → COV-FIRE  deductible=2.0  coinsurance=80.0
//   GUA-THEFT (active)            → COV-THEFT deductible=5.0  coinsurance=100.0
//   GUA-GLASS (active)            → COV-GLASS deductible=5.0  coinsurance=100.0
//   GUA-ELEC  (active)            → COV-ELEC  deductible=10.0 coinsurance=100.0
//   hasCatZone                    → COV-CAT   deductible=3.0  coinsurance=90.0
//   always                        → COV-BI    deductible=3.0  coinsurance=80.0  selected=false
public class CoverageDerivationService {

    // Coverage codes
    private static final String COV_FIRE  = "COV-FIRE";
    private static final String COV_THEFT = "COV-THEFT";
    private static final String COV_GLASS = "COV-GLASS";
    private static final String COV_ELEC  = "COV-ELEC";
    private static final String COV_CAT   = "COV-CAT";
    private static final String COV_BI    = "COV-BI";

    // Guarantee codes that trigger COV-FIRE
    private static final String GUA_FIRE = "GUA-FIRE";
    private static final String GUA_CONT = "GUA-CONT";
    private static final String GUA_THEFT = "GUA-THEFT";
    private static final String GUA_GLASS = "GUA-GLASS";
    private static final String GUA_ELEC  = "GUA-ELEC";

    // Deductible constants
    private static final BigDecimal DEDUCTIBLE_FIRE  = new BigDecimal("2.0");
    private static final BigDecimal DEDUCTIBLE_THEFT = new BigDecimal("5.0");
    private static final BigDecimal DEDUCTIBLE_GLASS = new BigDecimal("5.0");
    private static final BigDecimal DEDUCTIBLE_ELEC  = new BigDecimal("10.0");
    private static final BigDecimal DEDUCTIBLE_CAT   = new BigDecimal("3.0");
    private static final BigDecimal DEDUCTIBLE_BI    = new BigDecimal("3.0");

    // Coinsurance constants
    private static final BigDecimal COINSURANCE_80  = new BigDecimal("80.0");
    private static final BigDecimal COINSURANCE_90  = new BigDecimal("90.0");
    private static final BigDecimal COINSURANCE_100 = new BigDecimal("100.0");

    // Coverage descriptions
    private static final String DESC_FIRE  = "Incendio y riesgos adicionales";
    private static final String DESC_THEFT = "Robo con violencia";
    private static final String DESC_GLASS = "Vidrios";
    private static final String DESC_ELEC  = "Equipo electrónico";
    private static final String DESC_CAT   = "Zona catastrófica";
    private static final String DESC_BI    = "Interrupción de negocio";

    /**
     * Derives a list of CoverageOption from active guarantee codes and the catastrophic zone flag.
     *
     * @param activeGuaranteeCodes guarantee codes with insuredValue > 0
     * @param hasCatZone           true if any location has a non-null, non-blank catastrophicZone
     * @return derived coverage options; COV-BI is always included
     */
    public Map<String, String> knownDescriptions() {
        return Map.of(
                COV_FIRE,  DESC_FIRE,
                COV_THEFT, DESC_THEFT,
                COV_GLASS, DESC_GLASS,
                COV_ELEC,  DESC_ELEC,
                COV_CAT,   DESC_CAT,
                COV_BI,    DESC_BI
        );
    }

    public List<CoverageOption> deriveFrom(List<String> activeGuaranteeCodes, boolean hasCatZone) {
        Set<String> codes = new LinkedHashSet<>(activeGuaranteeCodes);
        List<CoverageOption> result = new ArrayList<>();

        // COV-FIRE: triggered by GUA-FIRE or GUA-CONT
        if (codes.contains(GUA_FIRE) || codes.contains(GUA_CONT)) {
            result.add(new CoverageOption(COV_FIRE, DESC_FIRE, true, DEDUCTIBLE_FIRE, COINSURANCE_80));
        }

        // COV-THEFT: triggered by GUA-THEFT
        if (codes.contains(GUA_THEFT)) {
            result.add(new CoverageOption(COV_THEFT, DESC_THEFT, true, DEDUCTIBLE_THEFT, COINSURANCE_100));
        }

        // COV-GLASS: triggered by GUA-GLASS
        if (codes.contains(GUA_GLASS)) {
            result.add(new CoverageOption(COV_GLASS, DESC_GLASS, true, DEDUCTIBLE_GLASS, COINSURANCE_100));
        }

        // COV-ELEC: triggered by GUA-ELEC
        if (codes.contains(GUA_ELEC)) {
            result.add(new CoverageOption(COV_ELEC, DESC_ELEC, true, DEDUCTIBLE_ELEC, COINSURANCE_100));
        }

        // COV-CAT: triggered by any location having a catastrophic zone
        if (hasCatZone) {
            result.add(new CoverageOption(COV_CAT, DESC_CAT, true, DEDUCTIBLE_CAT, COINSURANCE_90));
        }

        // COV-BI: always present, always selected=false
        result.add(new CoverageOption(COV_BI, DESC_BI, false, DEDUCTIBLE_BI, COINSURANCE_80));

        return result;
    }
}
