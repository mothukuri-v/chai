const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    role: { type: String, enum: ['customer', 'shop_owner', 'admin'], required: true, default: 'customer' },
    name: { type: String, required: true, trim: true },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    phone: { type: String, required: true, unique: true, trim: true },
    passwordHash: { type: String, required: true },
    status: { type: String, enum: ['ACTIVE', 'SUSPENDED'], default: 'ACTIVE' },
  },
  { timestamps: true }
);

module.exports = mongoose.model('User', userSchema);
