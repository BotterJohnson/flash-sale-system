package com.botter.shop.goods.mapper;

import com.botter.shop.goods.dto.BrandDTO;
import com.botter.shop.goods.model.Brand;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import java.util.Date;
import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    String ZongId = "Asia/Shanghai";
    BrandDTO toDTO(Brand brand);

    List<BrandDTO> toDTOList(List<Brand> brands);

    default Page<BrandDTO> toDTOPage(Page<Brand> brands) {
        return brands.map(this::toDTO);
    }

    default Date map(Instant value) {
        return value == null ? null : Date.from(value);
    }

    default Instant map(Date value) {
        return value == null ? null : value.toInstant();
    }
}
