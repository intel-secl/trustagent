/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mountwilson.trustagent.commands;

import com.intel.mtwilson.codec.HexUtil;
import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.ICommand;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.data.TADataContext;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.tpm.Tpm;
import com.intel.mtwilson.core.tpm.Tpm.Pcr;
import com.intel.mtwilson.core.tpm.Tpm.PcrBank;
import com.intel.mtwilson.core.tpm.model.TpmQuote;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dsmagadX
 */
public class GenerateQuoteCmd implements ICommand {

    Logger log = LoggerFactory.getLogger(getClass().getName());
    private final Pattern PCR_LIST_SSV = Pattern.compile("^[0-9][0-9 ]*$");

    private final TADataContext context;

    public GenerateQuoteCmd(TADataContext context) {
        this.context = context;
    }

    protected static byte[] hexStringToByteArray(String s) {
        int len = s.length();

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        String returnStr = "";
        for (int i = 0; i < b.length; i++) {
            String singleByte = Integer.toHexString(b[i] & 0xff);
            if (singleByte.length() != 2) {
                singleByte = "0" + singleByte;
            }
            returnStr = sb.append(singleByte).toString();
        }
        return returnStr;
    }

    @Override
    public void execute() throws TAException {
        List<String> tempSelectedBanks = new ArrayList<String>();
        String identityAuthKey = context.getIdentityAuthKey();
        String selectedPcrs = context.getSelectedPCRs();
        List<String> selectedBanks = context.getSelectedPcrBanks();
        if (!HexUtil.isHex(identityAuthKey)) {
            log.error("Aik secret password is not in hex format: {}", identityAuthKey);
            throw new IllegalArgumentException(String.format("Aik secret password is not in hex format."));
        }
        if (!PCR_LIST_SSV.matcher(selectedPcrs).matches()) {
            log.error("Selected PCRs do not match correct format: {}", selectedPcrs);
            throw new IllegalArgumentException(String.format("Selected PCRs do not match correct format."));
        }
        Tpm tpm;
        try {
            tpm = Tpm.open(Paths.get(Folders.application(), "bin"));
        } catch (IOException ex) {
            log.error("Error while creating instance of tpm provider", ex);
            throw new TAException(ErrorCode.ERROR, "Error while creating instance of tpm provider", ex);
        }
        String tpmVersion = tpm.getTpmVersion();
        log.debug("TPM version before calling: {} ", tpmVersion);
        // (String)"19" -> (int)19 -> (Tpm.Pcr)PCR19
        Set<Pcr> pcrs = Arrays.stream(selectedPcrs.split("\\s+")).map(Integer::parseInt).map(Pcr::fromInt).collect(Collectors.toCollection(HashSet::new));
        Set<PcrBank> pcrBanks;
        byte[] nonce = Base64.decodeBase64(context.getNonce());
        try {
            Set<PcrBank> supportedBanks = tpm.getPcrBanks();

            if (selectedBanks.isEmpty()) {
                log.debug("Supported Banks{}", supportedBanks);
                for (PcrBank temp : supportedBanks) {
                    selectedBanks.add(temp.toString());

                }
                log.debug("Selected Banks {}", selectedBanks);
                pcrBanks = supportedBanks;
            } else {
                
                pcrBanks = selectedBanks.stream().map(PcrBank::valueOf).collect(Collectors.toCollection(HashSet::new));
                
                if (!supportedBanks.containsAll(pcrBanks) && pcrBanks.size() == supportedBanks.size()) {
                    //This condition throws an error when the pcrbanks given as input are not supported on the platform
                    pcrBanks.removeAll(supportedBanks);
                    log.error("Unsupported PCR banks {}", pcrBanks);
                    throw new TAException(ErrorCode.ERROR, "Unsupported PCR banks " + pcrBanks);
                } else if (supportedBanks.containsAll(pcrBanks) && pcrBanks.size() == supportedBanks.size()) {
                    ////This condition will be executed in case when the pcrbanks given as input are supported on the platform
                   
                    pcrBanks = supportedBanks;
                    for (PcrBank temp : supportedBanks) {
                        tempSelectedBanks.add(temp.toString());
                    }
                    context.setSelectedPcrBanks(tempSelectedBanks);
                } else {
                    //This condition is for when only a subest of the pcrbanks given as input is supported on the platform
                   
                    pcrBanks = supportedBanks;
                    for (PcrBank temp : supportedBanks) {
                        tempSelectedBanks.add(temp.toString());
                    }
                    context.setSelectedPcrBanks(tempSelectedBanks);
                    context.setErrorCode(ErrorCode.UNSUPPORTED_PCR_BANK);
                }

                
            }
        } catch (IOException | Tpm.TpmException ex) {
            throw new TAException(ErrorCode.ERROR, "Failed to retrieve list of supported PCR Banks from host", ex);
        }
        try {
            TpmQuote quote = tpm.getQuote(pcrBanks, pcrs, FileUtils.readFileToByteArray(new File(context.getAikBlobFileName())), Hex.decode(identityAuthKey), nonce);
            context.setTpmQuote(quote);
        } catch (IOException | Tpm.TpmException ex) {
            log.error("Error while generating quote", ex);
            throw new TAException(ErrorCode.COMMAND_ERROR, "Error while generating quote", ex);
        }
    }
}
