package com.agricultural.assistplatform.controller.common;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.common.RegionService;
import com.agricultural.assistplatform.vo.common.RegionNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "通用-地区字典")
@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "获取地区级联数据（省/市/区/乡镇）")
    @GetMapping("/regions")
    public Result<List<RegionNodeVO>> regions() {
        return Result.ok(regionService.getRegionTree());
    }
}

