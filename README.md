Todo List Plugin for IDEA
=========================

This plugin is a simple todo list plugin for IDEA.

## Features

* Add todo item
* Delete todo item
* Mark todo item as done
* Save todo list to file
* Load todo list from file

## Environment

* This feature was developed alongside Github copilot

## Learnings

* How to create a plugin for IDEA
  * It's possible to create an empty project very easily from IDEA
  * It's possible to run the plugin from IDEA in a sandboxed environment
  * It's possible to develop the plugin in Java or Kotlin
* How to create a custom tool window
  * IDEA gives you a space of the UI and you can do whatever you want with it
  * It uses regular Swing components, so it's very easy to find good documentation
  * When IDEA prefers a custom component, it's very clear as the regular component is marked as deprecated. The new component is usually following the same API.
* How to create a custom action
  * It's possible to create a custom action and add it to the menu very easily using the plugin.xml file

## What needs to be tested more

* How to package the plugin
* How to install the plugin
* How to publish the plugin

