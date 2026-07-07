package com.inkpulse.features.order.rules;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.GhnDistrict;
import com.inkpulse.entities.GhnProvince;
import com.inkpulse.entities.GhnWard;
import com.inkpulse.entities.UserAddress;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.GhnDistrictRepository;
import com.inkpulse.repositories.GhnProvinceRepository;
import com.inkpulse.repositories.GhnWardRepository;
import com.inkpulse.repositories.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddressValidationRule implements IEligibilityRule<CreateOrderContext> {

    private final UserAddressRepository userAddressRepository;
    private final GhnProvinceRepository ghnProvinceRepository;
    private final GhnDistrictRepository ghnDistrictRepository;
    private final GhnWardRepository ghnWardRepository;

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        var cmd = ctx.getCommand();

        if (cmd.getAddressId() != null) {
            Optional<UserAddress> addressOpt = userAddressRepository.findById(cmd.getAddressId());
            if (addressOpt.isEmpty() || !addressOpt.get().getUser().getId().equals(cmd.getUserId())) {
                context.reject(OrderMessageConstants.ADDRESS_NOT_FOUND);
                return;
            }
            ctx.setAddress(addressOpt.get());
        } else {
            // New Address Validation
            if (cmd.getProvinceId() == null || cmd.getDistrictId() == null || cmd.getWardCode() == null
                    || cmd.getStreetAddress() == null || cmd.getStreetAddress().trim().isEmpty()
                    || cmd.getRecipientPhone() == null || cmd.getRecipientPhone().trim().isEmpty()
                    || cmd.getReceiverName() == null || cmd.getReceiverName().trim().isEmpty()) {
                context.reject(OrderMessageConstants.INVALID_ADDRESS);
                return;
            }

            Optional<GhnProvince> provinceOpt = ghnProvinceRepository.findById(cmd.getProvinceId());
            if (provinceOpt.isEmpty()) {
                context.reject("Không tìm thấy Tỉnh/Thành phố!");
                return;
            }

            Optional<GhnDistrict> districtOpt = ghnDistrictRepository.findById(cmd.getDistrictId());
            if (districtOpt.isEmpty() || !districtOpt.get().getProvince().getProvinceId().equals(cmd.getProvinceId())) {
                context.reject("Không tìm thấy Quận/Huyện!");
                return;
            }

            Optional<GhnWard> wardOpt = ghnWardRepository.findById(cmd.getWardCode());
            if (wardOpt.isEmpty() || !wardOpt.get().getDistrict().getDistrictId().equals(cmd.getDistrictId())) {
                context.reject("Không tìm thấy Phường/Xã!");
                return;
            }
        }
    }
}
