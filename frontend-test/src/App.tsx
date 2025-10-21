import { useState } from 'react';

function App() {
  const [topic, setTopic] = useState('');
  const [output, setOutput] = useState('');
  const [loading, setLoading] = useState(false);

  const generateQuestions = async () => {
    if (!topic.trim()) {
      setOutput('Please enter a topic first.');
      return;
    }

    setLoading(true);
    setOutput('Generating questions...');
    try {
      const response = await fetch(
        `http://localhost:8080/api/ai/generate?topic=${encodeURIComponent(topic)}`
      );

      if (!response.ok) throw new Error('Backend request failed');

      const text = await response.text();
      setOutput(text);
    } catch (err: any) {
      setOutput('Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '40px', fontFamily: 'Arial, sans-serif' }}>
      <h1>AI Question Generator</h1>
      <p>Enter a topic to generate exam-style questions using Ollama:</p>

      <textarea
        value={topic}
        onChange={(e) => setTopic(e.target.value)}
        rows={4}
        style={{ width: '100%', padding: '10px', fontSize: '1rem' }}
        placeholder="e.g. Photosynthesis"
      />
      <br />
      <button
        onClick={generateQuestions}
        disabled={loading}
        style={{
          marginTop: '10px',
          padding: '10px 20px',
          fontSize: '1rem',
          cursor: 'pointer',
        }}
      >
        {loading ? 'Generating...' : 'Generate'}
      </button>

      <h2>Output:</h2>
      <pre
        style={{
          background: '#f5f5f5',
          border: '1px solid #ccc',
          padding: '15px',
          whiteSpace: 'pre-wrap',
          minHeight: '100px',
        }}
      >
        {output}
      </pre>
    </div>
  );
}

export default App;
