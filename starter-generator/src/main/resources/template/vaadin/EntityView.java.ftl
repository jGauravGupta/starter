<#-- 
    Copyright 2024 the original author or authors from the Jeddict project (https://jeddict.github.io/).

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
-->
<#-- Detect which Vaadin field types are needed to produce minimal imports -->
<#assign hasDatePicker = false>
<#assign hasDateTimePicker = false>
<#assign hasNumberField = false>
<#assign hasTextField = false>
<#assign hasComboBox = false>
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi>
<#if model.getEntity(attribute.type)??>
<#assign hasComboBox = true>
<#elseif attribute.getType() == "LocalDate">
<#assign hasDatePicker = true>
<#elseif attribute.getType() == "LocalDateTime">
<#assign hasDateTimePicker = true>
<#elseif attribute.isNumber()>
<#assign hasNumberField = true>
<#else>
<#assign hasTextField = true>
</#if>
</#if>
</#list>
<#-- Collect unique related-entity types to avoid duplicate imports/fields -->
<#assign relatedTypes = []>
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi && model.getEntity(attribute.type)??>
<#if !relatedTypes?seq_contains(attribute.type)>
<#assign relatedTypes = relatedTypes + [attribute.type]>
</#if>
</#if>
</#list>
package ${package};

import ${EntityClass_FQN};
import ${EntityRepository_FQN};
import ${model.importPrefix}.annotation.PostConstruct;
import ${model.importPrefix}.inject.Inject;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
<#if hasDatePicker>
import com.vaadin.flow.component.datepicker.DatePicker;
</#if>
<#if hasDateTimePicker>
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
</#if>
<#if hasNumberField>
import com.vaadin.flow.component.textfield.NumberField;
</#if>
<#if hasTextField>
import com.vaadin.flow.component.textfield.TextField;
</#if>
<#if hasComboBox>
import com.vaadin.flow.component.combobox.ComboBox;
</#if>
<#-- Imports for related-entity repositories (one per unique related type) -->
<#list relatedTypes as relatedType>
import ${EntityRepository_package}.${relatedType}${EntityRepositorySuffix};
</#list>

@PageTitle("${entity.getTitle()?json_string}")
@Route(value = "${entityNameLowerCase}", layout = MainLayout.class)
public class ${entity.getClassName()}View extends VerticalLayout {

    @Inject
    private ${EntityRepository} ${entityRepository};
<#list relatedTypes as relatedType>
    @Inject
    private ${relatedType}${EntityRepositorySuffix} ${relatedType?uncap_first}${EntityRepositorySuffix};
</#list>

    private final Grid<${EntityClass}> grid = new Grid<>(${EntityClass}.class, false);

<#-- Form field declarations -->
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi>
<#if model.getEntity(attribute.type)??>
    private final ComboBox<${attribute.type}> ${attribute.name}Field = new ComboBox<>("${attribute.getStartCaseName()?json_string}");
<#elseif attribute.getType() == "LocalDate">
    private final DatePicker ${attribute.name}Field = new DatePicker("${attribute.getStartCaseName()?json_string}");
<#elseif attribute.getType() == "LocalDateTime">
    private final DateTimePicker ${attribute.name}Field = new DateTimePicker("${attribute.getStartCaseName()?json_string}");
<#elseif attribute.isNumber()>
    private final NumberField ${attribute.name}Field = new NumberField("${attribute.getStartCaseName()?json_string}");
<#else>
    private final TextField ${attribute.name}Field = new TextField("${attribute.getStartCaseName()?json_string}");
</#if>
</#if>
</#list>

    private ${EntityClass} current${EntityClass} = new ${EntityClass}();

    @PostConstruct
    public void init() {
        setSizeFull();
        add(new H2("${entity.getTitle()?json_string}"));
        add(new Paragraph("${entity.getDescription()?json_string}"));
        configureGrid();
        Button addButton = new Button("Add ${entity.getTitle()?json_string}", e -> openDialog(new ${EntityClass}()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(addButton, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
<#list entity.attributes as attribute>
<#if !attribute.multi>
<#if model.getEntity(attribute.type)??>
        grid.addColumn(item -> item.get${attribute.getTitleCaseName()}() != null
                ? item.get${attribute.getTitleCaseName()}().toString() : "")
            .setHeader("${attribute.getStartCaseName()?json_string}");
<#else>
        grid.addColumn(${EntityClass}::get${attribute.getTitleCaseName()}).setHeader("${attribute.getStartCaseName()?json_string}");
</#if>
</#if>
</#list>
        grid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button("Edit", e -> openDialog(item));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button deleteButton = new Button("Delete", e -> {
                ${entityRepository}.remove(${entityRepository}.find(item.${pkGetter}()));
                refreshGrid();
                Notification.show("${entity.getTitle()?json_string} deleted.", 3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void openDialog(${EntityClass} ${entityNameLowerCase}) {
        current${EntityClass} = ${entityNameLowerCase};
        boolean isNew = ${entityNameLowerCase}.${pkGetter}() == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Add ${entity.getTitle()?json_string}" : "Edit ${entity.getTitle()?json_string}");

<#-- Populate form fields from entity -->
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi>
<#if model.getEntity(attribute.type)??>
        ${attribute.name}Field.setItems(${attribute.type?uncap_first}${EntityRepositorySuffix}.findAll());
        ${attribute.name}Field.setItemLabelGenerator(${attribute.type}::toString);
        ${attribute.name}Field.setValue(${entityNameLowerCase}.get${attribute.getTitleCaseName()}());
<#elseif attribute.getType() == "LocalDate" || attribute.getType() == "LocalDateTime">
        ${attribute.name}Field.setValue(${entityNameLowerCase}.get${attribute.getTitleCaseName()}());
<#elseif attribute.isNumber()>
<#if ["int", "long", "float", "double", "byte", "short"]?seq_contains(attribute.getType())>
        ${attribute.name}Field.setValue((double) ${entityNameLowerCase}.get${attribute.getTitleCaseName()}());
<#else>
        ${attribute.name}Field.setValue(${entityNameLowerCase}.get${attribute.getTitleCaseName()}() != null
                ? ((Number) ${entityNameLowerCase}.get${attribute.getTitleCaseName()}()).doubleValue() : null);
</#if>
<#else>
        ${attribute.name}Field.setValue(${entityNameLowerCase}.get${attribute.getTitleCaseName()}() != null
                ? String.valueOf(${entityNameLowerCase}.get${attribute.getTitleCaseName()}()) : "");
</#if>
</#if>
</#list>

        FormLayout formLayout = new FormLayout();
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi>
        formLayout.add(${attribute.name}Field);
</#if>
</#list>

        Button saveButton = new Button("Save", e -> {
            try {
<#list entity.attributes as attribute>
<#if !attribute.primaryKey && !attribute.multi>
<#if model.getEntity(attribute.type)??>
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue());
<#elseif attribute.getType() == "LocalDate" || attribute.getType() == "LocalDateTime">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue());
<#elseif attribute.getType() == "int">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().intValue() : 0);
<#elseif attribute.getType() == "Integer">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().intValue() : null);
<#elseif attribute.getType() == "long">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().longValue() : 0L);
<#elseif attribute.getType() == "Long">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().longValue() : null);
<#elseif attribute.getType() == "float">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().floatValue() : 0.0f);
<#elseif attribute.getType() == "Float">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().floatValue() : null);
<#elseif attribute.getType() == "double">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue() : 0.0);
<#elseif attribute.getType() == "Double">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue());
<#elseif attribute.getType() == "byte">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().byteValue() : (byte) 0);
<#elseif attribute.getType() == "Byte">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().byteValue() : null);
<#elseif attribute.getType() == "short">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().shortValue() : (short) 0);
<#elseif attribute.getType() == "Short">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().shortValue() : null);
<#elseif attribute.getType() == "java.math.BigDecimal">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? java.math.BigDecimal.valueOf(${attribute.name}Field.getValue()) : null);
<#elseif attribute.getType() == "java.math.BigInteger">
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? java.math.BigInteger.valueOf(${attribute.name}Field.getValue().longValue()) : null);
<#elseif attribute.isNumber()>
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue() != null ? ${attribute.name}Field.getValue().intValue() : 0);
<#else>
                current${EntityClass}.set${attribute.getTitleCaseName()}(${attribute.name}Field.getValue());
</#if>
</#if>
</#list>
                if (isNew) {
                    ${entityRepository}.create(current${EntityClass});
                } else {
                    ${entityRepository}.edit(current${EntityClass});
                }
                refreshGrid();
                dialog.close();
                Notification.show("${entity.getTitle()?json_string} saved.", 3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(${entityRepository}.findAll());
    }
}
