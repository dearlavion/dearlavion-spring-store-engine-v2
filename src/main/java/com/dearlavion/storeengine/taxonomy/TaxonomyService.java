package com.dearlavion.storeengine.taxonomy;

import com.dearlavion.storeengine.common.exception.ConflictException;
import com.dearlavion.storeengine.common.exception.NotFoundException;
import com.dearlavion.storeengine.taxonomy.model.AxisOrder;
import com.dearlavion.storeengine.taxonomy.model.TaxonomyValue;
import com.dearlavion.storeengine.taxonomy.request.CreateTaxonomyValueRequest;
import com.dearlavion.storeengine.taxonomy.request.UpdateTaxonomyValueRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TaxonomyService {

    private static final Set<String> VALID_AXES = Set.of(
            "destination", "season", "party", "transportation", "activity", "kitCategory", "duration", "gender"
    );

    private final TaxonomyValueRepository taxonomyValueRepository;
    private final AxisOrderRepository axisOrderRepository;

    public TaxonomyService(TaxonomyValueRepository taxonomyValueRepository, AxisOrderRepository axisOrderRepository) {
        this.taxonomyValueRepository = taxonomyValueRepository;
        this.axisOrderRepository = axisOrderRepository;
    }

    /** Every value, every axis — the frontend fetches this once and groups/sorts client-side. */
    public List<TaxonomyValue> listAll() {
        return taxonomyValueRepository.findAllByOrderByAxisAscOrderAsc();
    }

    public TaxonomyValue create(CreateTaxonomyValueRequest input) {
        TaxonomyValue value = new TaxonomyValue();
        value.setAxis(input.axis());
        value.setValue(input.value());
        value.setOrder(input.order() != null ? input.order() : 0);
        value.setEmoji(input.emoji());
        value.setSubtext(input.subtext());
        try {
            return taxonomyValueRepository.save(value);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("\"" + input.value() + "\" already exists in this list");
        }
    }

    public TaxonomyValue update(String id, UpdateTaxonomyValueRequest patch) {
        TaxonomyValue value = taxonomyValueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Taxonomy value not found"));
        if (patch.value() != null) value.setValue(patch.value());
        if (patch.order() != null) value.setOrder(patch.order());
        if (patch.emoji() != null) value.setEmoji(patch.emoji());
        if (patch.subtext() != null) value.setSubtext(patch.subtext());
        return taxonomyValueRepository.save(value);
    }

    public void delete(String id) {
        taxonomyValueRepository.deleteById(id);
    }

    public TaxonomyValue findById(String id) {
        return taxonomyValueRepository.findById(id).orElse(null);
    }

    /** Idempotent upsert by (axis, value) — used by the seed script. */
    public void upsert(String axis, String value, Integer order, String emoji, String subtext, String code) {
        TaxonomyValue existing = taxonomyValueRepository.findByAxisAndValue(axis, value).orElseGet(TaxonomyValue::new);
        existing.setAxis(axis);
        existing.setValue(value);
        existing.setOrder(order != null ? order : 0);
        existing.setEmoji(emoji);
        existing.setSubtext(subtext);
        existing.setCode(code);
        taxonomyValueRepository.save(existing);
    }

    /** The 8 axes' display/step order. Falls back to the out-of-the-box order until an admin has
     * ever saved a custom one. */
    public List<String> getAxisOrder() {
        return axisOrderRepository.findById(AxisOrder.SINGLETON_ID)
                .map(AxisOrder::getOrder)
                .filter(order -> !order.isEmpty())
                .orElse(AxisOrder.DEFAULT_ORDER);
    }

    public List<String> updateAxisOrder(List<String> order) {
        for (String axis : order) {
            if (!VALID_AXES.contains(axis)) {
                throw new IllegalArgumentException("Unknown axis: " + axis);
            }
        }
        AxisOrder axisOrder = axisOrderRepository.findById(AxisOrder.SINGLETON_ID).orElseGet(AxisOrder::new);
        axisOrder.setOrder(order);
        axisOrderRepository.save(axisOrder);
        return order;
    }
}
