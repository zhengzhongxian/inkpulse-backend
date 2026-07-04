package com.inkpulse.features.user.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.AddressMessageConstants;
import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.user.commands.CreateUserAddressCommand;
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
public class CreateUserAddressCommandHandler implements Command.CommandHandler<CreateUserAddressCommand, UserProfileCacheDto> {

    private final UserRepository userRepository;
    private final GhnProvinceRepository provinceRepository;
    private final GhnDistrictRepository districtRepository;
    private final GhnWardRepository wardRepository;
    private final UserAddressRepository userAddressRepository;
    private final SectionCacheService sectionCache;
    private final Pipeline pipeline;

    @Override
    @Transactional
    public UserProfileCacheDto handle(CreateUserAddressCommand cmd) {
        User user = userRepository.findById(cmd.getUserId())
                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.USER_NOT_FOUND, "USER_NOT_FOUND"));

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

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientPhone(cmd.getRecipientPhone())
                .province(province)
                .district(district)
                .ward(ward)
                .streetAddress(cmd.getStreetAddress())
                .addressLabel(cmd.getAddressLabel() != null && !cmd.getAddressLabel().isBlank() ? cmd.getAddressLabel() : "Nhà riêng")
                .lastUsedAt(LocalDateTime.now())
                .build();
        userAddressRepository.save(address);

        // Synchronize in-memory bi-directional relationship for correct cache serialization in the same transaction
        if (user.getAddresses() != null) {
            user.getAddresses().add(address);
        }

        // Evict profile cache so list is refreshed
        sectionCache.remove(user.getId().toString(), UserProfileCacheDto.class);

        return pipeline.send(new GetUserProfileByUserIdQuery(user.getId()));
    }
}
