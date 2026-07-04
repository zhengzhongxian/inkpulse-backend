package com.inkpulse.features.user.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.AddressMessageConstants;
import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.user.commands.UpdateUserAddressCommand;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.features.user.queries.GetUserProfileByUserIdQuery;
import com.inkpulse.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import an.awesome.pipelinr.Pipeline;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserAddressCommandHandler implements Command.CommandHandler<UpdateUserAddressCommand, UserProfileCacheDto> {

    private final UserAddressRepository userAddressRepository;
    private final GhnProvinceRepository provinceRepository;
    private final GhnDistrictRepository districtRepository;
    private final GhnWardRepository wardRepository;
    private final SectionCacheService sectionCache;
    private final Pipeline pipeline;

    @Override
    @Transactional
    public UserProfileCacheDto handle(UpdateUserAddressCommand cmd) {
        UserAddress address = userAddressRepository.findById(cmd.getAddressId())
                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.ADDRESS_NOT_FOUND, "ADDRESS_NOT_FOUND"));

        if (!address.getUser().getId().equals(cmd.getUserId())) {
            throw new BusinessValidationException(UserMessageConstants.ACCESS_DENIED, "ACCESS_DENIED");
        }

        GhnProvince province = provinceRepository.findById(cmd.getProvinceId())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.PROVINCE_NOT_FOUND, "PROVINCE_NOT_FOUND"));
        GhnDistrict district = districtRepository.findById(cmd.getDistrictId())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.DISTRICT_NOT_FOUND, "DISTRICT_NOT_FOUND"));
        GhnWard ward = wardRepository.findById(cmd.getWardCode())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.WARD_NOT_FOUND, "WARD_NOT_FOUND"));

        // Validate hierarchy
        if (!district.getProvince().getProvinceId().equals(province.getProvinceId())) {
            throw new BusinessValidationException(AddressMessageConstants.INVALID_DISTRICT_PROVINCE, "INVALID_DISTRICT_PROVINCE");
        }
        if (!ward.getDistrict().getDistrictId().equals(district.getDistrictId())) {
            throw new BusinessValidationException(AddressMessageConstants.INVALID_WARD_DISTRICT, "INVALID_WARD_DISTRICT");
        }

        address.setRecipientPhone(cmd.getRecipientPhone());
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setStreetAddress(cmd.getStreetAddress());
        address.setAddressLabel(cmd.getAddressLabel() != null && !cmd.getAddressLabel().isBlank() ? cmd.getAddressLabel() : "Nhà riêng");
        address.setLastUsedAt(LocalDateTime.now());
        
        userAddressRepository.save(address);

        // Evict profile cache so list is refreshed
        sectionCache.remove(cmd.getUserId().toString(), UserProfileCacheDto.class);

        return pipeline.send(new GetUserProfileByUserIdQuery(cmd.getUserId()));
    }
}
