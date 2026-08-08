package com.dearlavion.storeengine.stats;

/** Maps one row of the $group stage's output — `id` binds from the group's `_id` key via Spring
 * Data's id-property convention (the group key really is the document's `_id` here, unlike
 * ProductItemQueryBuilder's aliased "id" field — see that class's comment for the contrast). */
record SalesRow(String id, long unitsSold, long orderCount, double revenue) {
}
