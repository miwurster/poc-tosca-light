package org.eclipse.winery.model.tosca;

import java.util.Objects;

import org.eclipse.winery.model.tosca.visitor.Visitor;

public class TGroupType extends TEntityType {
    

    public TGroupType() {
    }

    public TGroupType(TGroupType.Builder builder) {
        super(builder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TGroupType)) return false;
        if (!super.equals(o)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    

    public static class Builder extends TEntityType.Builder<TGroupType.Builder> {

        public Builder(String name) {
            super(name);
        }

        public Builder(TEntityType entityType) {
            super(entityType);
        }

        

        @Override
        public TGroupType.Builder self() {
            return this;
        }

        public TGroupType build() {
            return new TGroupType(this);
        }
    }
}
