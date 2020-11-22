resource "azurerm_resource_group" "service-bus-bug-rg" {
  name     = "service-bus-bug-rg"
  location = "East US"

    tags = {
      usage = "Investigate service bus bug"
    }
}

resource "azurerm_servicebus_namespace" "service-bus-bug-srvb" {
  name                = "service-bus-bug-srvb"
  location            = azurerm_resource_group.service-bus-bug-rg.location
  resource_group_name = azurerm_resource_group.service-bus-bug-rg.name
  sku                 = "Standard"

  tags = {
    usage = "Investigate service bus bug"
  }
}

resource "azurerm_servicebus_topic" "service-bus-bug-topic" {
  name                = "service-bus-bug-topic"
  resource_group_name = azurerm_resource_group.service-bus-bug-rg.name
  namespace_name      = azurerm_servicebus_namespace.service-bus-bug-srvb.name

  enable_partitioning = true
}

resource "azurerm_servicebus_subscription" "service-bus-bug-subscription" {
  name                = "service-bus-bug-subscription"
  resource_group_name = azurerm_resource_group.service-bus-bug-rg.name
  namespace_name      = azurerm_servicebus_namespace.service-bus-bug-srvb.name
  topic_name          = azurerm_servicebus_topic.service-bus-bug-topic.name

  max_delivery_count  = 1
  requires_session = true
}