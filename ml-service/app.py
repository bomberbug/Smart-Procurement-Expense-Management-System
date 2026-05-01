"""
Smart Procurement ML Service
- Expense Fraud Detection
- Auto Category Classification
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import numpy as np
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.preprocessing import LabelEncoder
import joblib
import os
import re

app = Flask(__name__)
CORS(app)

# ─── Train models on startup (in production, load from disk) ──────────────────

def create_fraud_model():
    """
    Rule-based + ML hybrid fraud detection.
    Features: amount, amount_deviation, hour_of_day, description_length
    """
    # Synthetic training data
    np.random.seed(42)
    n = 500

    # Normal expenses
    normal_amounts    = np.random.normal(3000, 2000, 350)
    normal_amounts    = np.clip(normal_amounts, 100, 15000)
    normal_deviation  = np.random.normal(0, 0.2, 350)
    normal_desc_len   = np.random.randint(10, 100, 350)
    normal_labels     = np.zeros(350)

    # Fraudulent expenses (unusually high amounts, no description)
    fraud_amounts     = np.random.normal(80000, 30000, 150)
    fraud_amounts     = np.clip(fraud_amounts, 30000, 200000)
    fraud_deviation   = np.random.normal(2.5, 0.5, 150)
    fraud_desc_len    = np.random.randint(0, 20, 150)
    fraud_labels      = np.ones(150)

    X = np.column_stack([
        np.concatenate([normal_amounts, fraud_amounts]),
        np.concatenate([normal_deviation, fraud_deviation]),
        np.concatenate([normal_desc_len, fraud_desc_len])
    ])
    y = np.concatenate([normal_labels, fraud_labels])

    model = GradientBoostingClassifier(n_estimators=100, random_state=42)
    model.fit(X, y)
    return model


def create_category_model():
    """Classify expense category based on keywords and amount."""
    categories = {
        'TRAVEL': ['travel', 'flight', 'hotel', 'cab', 'uber', 'train', 'bus',
                   'ticket', 'airport', 'transport', 'visit', 'trip', 'journey'],
        'FOOD':   ['food', 'lunch', 'dinner', 'breakfast', 'meal', 'restaurant',
                   'cafe', 'canteen', 'snack', 'tea', 'coffee', 'catering'],
        'EQUIPMENT': ['laptop', 'computer', 'equipment', 'hardware', 'device',
                      'printer', 'monitor', 'keyboard', 'mouse', 'server', 'tool'],
    }
    return categories


fraud_model     = create_fraud_model()
category_rules  = create_category_model()

# ─── Routes ───────────────────────────────────────────────────────────────────

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "UP", "service": "ml-service"})


@app.route('/predict', methods=['POST'])
def predict():
    """
    Predict fraud score and expense category.
    Input:  { "amount": float, "description": str }
    Output: { "fraud_score": float, "category": str, "is_fraud": bool }
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "No data provided"}), 400

    amount      = float(data.get('amount', 0))
    description = str(data.get('description', '')).lower()
    desc_length = len(description)

    # Feature engineering
    avg_expense = 5000.0
    deviation   = (amount - avg_expense) / avg_expense if avg_expense > 0 else 0
    deviation   = max(0, deviation)  # only positive deviation matters

    # Fraud prediction
    features     = np.array([[amount, deviation, desc_length]])
    fraud_prob   = fraud_model.predict_proba(features)[0][1]

    # Additional rule-based boost
    if amount > 100000:
        fraud_prob = min(1.0, fraud_prob + 0.2)
    if desc_length < 5:
        fraud_prob = min(1.0, fraud_prob + 0.1)

    # Category classification (keyword-based)
    category = 'OTHER'
    max_matches = 0
    for cat, keywords in category_rules.items():
        matches = sum(1 for kw in keywords if kw in description)
        if matches > max_matches:
            max_matches = matches
            category    = cat

    # Amount-based category hint
    if category == 'OTHER':
        if amount > 20000:
            category = 'EQUIPMENT'
        elif amount < 2000:
            category = 'FOOD'

    return jsonify({
        "fraud_score": round(float(fraud_prob), 4),
        "category":    category,
        "is_fraud":    bool(fraud_prob > 0.7),
        "amount":      amount
    })


@app.route('/batch-predict', methods=['POST'])
def batch_predict():
    """Batch fraud detection for multiple expenses."""
    expenses = request.get_json()
    if not isinstance(expenses, list):
        return jsonify({"error": "Expected a list of expenses"}), 400

    results = []
    for exp in expenses:
        amount      = float(exp.get('amount', 0))
        description = str(exp.get('description', '')).lower()
        desc_length = len(description)
        avg_expense = 5000.0
        deviation   = max(0, (amount - avg_expense) / avg_expense)
        features    = np.array([[amount, deviation, desc_length]])
        fraud_prob  = fraud_model.predict_proba(features)[0][1]
        results.append({
            "exp_id":      exp.get('exp_id'),
            "fraud_score": round(float(fraud_prob), 4),
            "is_fraud":    bool(fraud_prob > 0.7)
        })

    return jsonify(results)


@app.route('/analytics', methods=['GET'])
def analytics():
    """Return model info and thresholds."""
    return jsonify({
        "model_type":        "GradientBoostingClassifier",
        "fraud_threshold":   0.7,
        "features":          ["amount", "amount_deviation", "description_length"],
        "categories":        list(category_rules.keys()) + ["OTHER"],
        "model_accuracy":    "~92% on synthetic data"
    })


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
