View = require './view'

class TasksView extends View

    templateTasksActive: require './templates/tasksActive'
    templateTasksScheduled: require './templates/tasksScheduled'

    render: (tasksFiltered) =>
        @collection = app.collections.tasksActive
        template = @templateTasksActive

        if tasksFiltered is 'scheduled'
            @collection = app.collections.tasksScheduled
            template = @templateTasksScheduled

        context =
            tasks: _.pluck(@collection.sort().models, 'attributes').reverse()

        @$el.html template context

        @setupEvents()
        @setUpSearchEvents()
        utils.setupSortableTables()

    setupEvents: ->
        @$el.find('.view-json').unbind('click').click (event) ->
            utils.viewJSON 'task', $(event.target).data('task-id')

        $removeLinks = @$el.find('[data-action="remove"]')

        $removeLinks.unbind('click').on 'click', (e) =>
            row = $(e.target).parents('tr')
            taskModel = @collection.get($(e.target).data('task-id'))

            vex.dialog.confirm
                message: "<p>Are you sure you want to delete the task:</p><pre>#{ taskModel.get('id') }</pre>"
                callback: (confirmed) =>
                    return unless confirmed
                    taskModel.destroy()
                    delete app.allTasks[taskModel.get('id')] # TODO - move to model on destroy?
                    @collection.remove(taskModel)
                    row.remove()

    setUpSearchEvents: =>
        $search = @$el.find('input[type="search"]')
        $search.focus() if $(window).width() > 568

        $rows = @$el.find('tbody > tr')

        lastText = _.trim $search.val()

        $search.unbind().on 'change keypress paste focus textInput input click keydown', =>
            text = _.trim $search.val()

            if text is ''
                $rows.removeClass('filtered')

            if text isnt lastText
                $rows.each ->
                    $row = $(@)

                    if not _.string.contains $row.data('task-id').toLowerCase(), text.toLowerCase()
                        $row.addClass('filtered')
                    else
                        $row.removeClass('filtered')

module.exports = TasksView