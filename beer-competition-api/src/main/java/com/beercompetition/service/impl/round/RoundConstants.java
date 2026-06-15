package com.beercompetition.service.impl.round;

/**
 * 轮次评审模块的共享常量。
 *
 * <p>这些值原本散落在 RoundServiceImpl 中，集中后可以避免多个拆分组件重复定义魔法值。</p>
 */
public final class RoundConstants {

    public static final int FLAG_TRUE = 1;
    public static final int FLAG_FALSE = 0;
    public static final int FULL_PROGRESS = 100;

    public static final String CATEGORY_MODE_EMPTY = "EMPTY";
    public static final String CATEGORY_MODE_MIXED = "MIXED";
    public static final String CATEGORY_MODE_CATEGORY = "CATEGORY";

    public static final String SLOT_GOLD = "金奖";
    public static final String SLOT_SILVER = "银奖";
    public static final String SLOT_BRONZE = "铜奖";

    private RoundConstants() {
    }
}
