package cn.edu.nuc.onboarding.system.common;

import java.util.List;

public record PageResult<T>(int pageNum, int pageSize, long total, List<T> list) {
}
