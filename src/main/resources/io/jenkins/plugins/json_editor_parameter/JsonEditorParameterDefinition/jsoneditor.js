Behaviour.specify(".editor-holder", "json-editor-parameter", 0, function(element) {
    const holderElement = element;
    const name = holderElement.dataset.name;
    const valueElement = document.getElementById(`editor:${name}:value`)

    const options = JSON.parse(document.getElementById(`editor:${name}:options`).textContent)
    console.log('Options:', options)
    
    const initialJson = options.json || {}
    console.log('Initial JSON:', initialJson)
    
    valueElement.value = JSON.stringify(initialJson)

    // Create editor with schema and initial json
    const editorOptions = {
        schema: options.schema,
        mode: 'tree',
        onChange: function() {
            valueElement.value = JSON.stringify(editor.getValue())
        }
    }
    console.log('Editor Options:', editorOptions)
    
    const editor = new JSONEditor(holderElement, editorOptions, initialJson)
    console.log('Editor created with:', {
        schema: editorOptions.schema,
        initialJson: initialJson
    })
});