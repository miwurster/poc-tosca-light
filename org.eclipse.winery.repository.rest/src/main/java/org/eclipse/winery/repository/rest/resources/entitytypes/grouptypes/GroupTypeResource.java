package org.eclipse.winery.repository.rest.resources.entitytypes.grouptypes;

import org.eclipse.winery.common.ids.definitions.GroupTypeId;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TGroupType;
import org.eclipse.winery.repository.rest.resources.entitytypes.EntityTypeResource;

public class GroupTypeResource extends EntityTypeResource {

    public GroupTypeResource(GroupTypeId id) {
        super(id);
    }

    /**
     * Convenience method to avoid casting at the caller's side.
     */
    public TGroupType getGroupType() {
        return (TGroupType) this.getElement();
    }

    @Override
    protected TExtensibleElements createNewElement() {
        return new TGroupType();
    }
}
